package columnar;

import static global.AttrType.attrInteger;
import static global.AttrType.attrNull;
import static global.AttrType.attrReal;
import static global.AttrType.attrString;
import static global.AttrType.attrSymbol;

import bitmap.BitMapFile;
import bitmap.BitmapScan;
import btree.ConstructPageException;
import btree.GetFileEntryException;
import diskmgr.Page;
import global.AttrOperator;
import global.AttrType;
import global.PageId;
import global.RID;
import global.TupleOrder;
import heap.DataPageInfo;
import heap.FieldNumberOutOfBoundException;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.HFPage;
import heap.Heapfile;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.InvalidTypeException;
import heap.Tuple;
import iterator.CondExpr;
import iterator.FileScan;
import iterator.FileScanException;
import iterator.FldSpec;
import iterator.InvalidRelation;
import iterator.Iterator;
import iterator.RelSpec;
import iterator.Sort;
import iterator.SortException;
import iterator.TupleUtilsException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Util {

  public static ValueClass valueClassFactory(AttrType type) {
    ValueClass val = null;
    switch (type.attrType) {
      case attrString:
        val = new StringValue();
        break;
      case attrInteger:
        val = new IntegerValue();
        break;
      case attrReal:
        val = new FloatValue();
        break;
      case attrSymbol:
        //TODO: Find out whether this is character and implement it.
        break;
      case attrNull:
        //TODO: Find out the right type and implement it.
        break;
    }

    return val;
  }


  public static Tuple createColumnarTuple(Tuple rowTuple, int fieldNo, AttrType attrType)
      throws InvalidTupleSizeException, IOException, InvalidTypeException, FieldNumberOutOfBoundException {
    //TODO: Init tuple with specific size rather than default 1024
    Tuple columnTuple = new Tuple();
    short colTupFields = 1;
    switch (attrType.attrType) {
      case attrString:
        String strVal = rowTuple.getStrFld(fieldNo);
        columnTuple
            .setHdr(colTupFields, new AttrType[]{attrType}, new short[]{(short) strVal.length()});
        columnTuple.setStrFld(1, strVal);
        break;
      case attrInteger:
        int intVal = rowTuple.getIntFld(fieldNo);
        columnTuple.setHdr(colTupFields, new AttrType[]{attrType}, new short[]{});
        columnTuple.setIntFld(1, intVal);
        break;
      case attrReal:
        float floatVal = rowTuple.getFloFld(fieldNo);
        columnTuple.setHdr(colTupFields, new AttrType[]{attrType}, new short[]{});
        columnTuple.setFloFld(1, floatVal);
        break;
      case attrSymbol:
        //TODO: Find out whether this is character and implement it.
        break;
      case attrNull:
        //TODO: Find out the right type and implement it.
        break;
    }

    return columnTuple;
  }

  public static String getBitAsString(byte dataByte, int position) {
    String s1 = String.format("%8s", Integer.toBinaryString(dataByte & 0xFF)).replace(' ', '0');
    return new String("" + s1.charAt(position));
  }

  public static void printBitsInByte(byte[] val) {
    for (int i = 0; i < val.length; i++) {
      byte b1 = val[i];
      String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
      System.out.println(s1); // 10000001

      //System.out.println(Integer.toBinaryString( (int) val[i]));
    }
  }

  public static RID getRIDFromPosition(int position, Heapfile hf)
      throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
    int curcount = position;
    PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
    HFPage currentDirPage = new HFPage();
    PageId nextDirPageId = new PageId(0);

    Page pageinbuffer = new Page();

    boolean flag = true;

    RID recid = new RID();
    DataPageInfo dpinfo = new DataPageInfo();
    while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
      hf.pinPage(currentDirPageId, currentDirPage, false);

      Tuple atuple;
      for (recid = currentDirPage.firstRecord();
          recid != null;  // rid==NULL means no more record
          recid = currentDirPage.nextRecord(recid)) {
        atuple = currentDirPage.getRecord(recid);
        dpinfo = new DataPageInfo(atuple);

        if (curcount - dpinfo.recct >= 0) {
          curcount -= dpinfo.recct;
        } else if (curcount == 0) {
          flag = false;
          break;
        } else {
          flag = false;
          break;
        }
      }

      // ASSERTIONS: no more record
      // - we have read all datapage records on
      //   the current directory page.

      if (flag) {
        nextDirPageId = currentDirPage.getNextPage();
        hf.unpinPage(currentDirPageId, false /*undirty*/);
        currentDirPageId.pid = nextDirPageId.pid;
      }
    }
    //recid points to data page with the position

    HFPage currentDataPage = new HFPage();
    PageId currentDataPageId = new PageId(dpinfo.getPageId().pid);
    hf.pinPage(currentDataPageId, currentDataPage, false/*Rdisk*/);

    RID record = new RID();
    for (record = currentDataPage.firstRecord();
        record != null && curcount > 0;  // rid==NULL means no more record
        record = currentDataPage.nextRecord(record)) {
      curcount--;
    }
//        RID record = currentDataPage.firstRecord();
//        curcount--;
//        while( record != null && curcount>=0) {
//            record = currentDataPage.nextRecord(record);
//            curcount--;
//        }
    hf.unpinPage(currentDataPageId, false);

    return record;
  }


  public static Tuple getTupleFromPosition(int position, Heapfile hf)
      throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
    int curcount = position;
    PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
    HFPage currentDirPage = new HFPage();
    PageId nextDirPageId = new PageId(0);

    Page pageinbuffer = new Page();

    boolean flag = true;
    DataPageInfo dpinfo = null;

    RID recid = new RID();
    while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
      hf.pinPage(currentDirPageId, currentDirPage, false);

      Tuple atuple;
      for (recid = currentDirPage.firstRecord();
          recid != null;  // rid==NULL means no more record
          recid = currentDirPage.nextRecord(recid)) {
        atuple = currentDirPage.getRecord(recid);
        dpinfo = new DataPageInfo(atuple);

        if (curcount - dpinfo.recct > 0) {
          curcount -= dpinfo.recct;
        } else {
          flag = false;
          break;
        }
      }

      // ASSERTIONS: no more record
      // - we have read all datapage records on
      //   the current directory page.

      if (flag) {
        nextDirPageId = currentDirPage.getNextPage();
        hf.unpinPage(currentDirPageId, false /*undirty*/);
        currentDirPageId.pid = nextDirPageId.pid;
      }
    }

    HFPage currentDataPage = new HFPage();
    PageId currentDataPageId = dpinfo.getPageId();
    hf.pinPage(currentDataPageId, currentDataPage, false/*Rdisk*/);

    Tuple nextTuple = new Tuple();
    for (recid = currentDataPage.firstRecord();
        recid != null && curcount >= 0;  // rid==NULL means no more record
        recid = currentDataPage.nextRecord(recid)) {
      nextTuple = currentDataPage.getRecord(recid);
      curcount--;
    }
    try {
      //TODO: Remove the try catch block
      hf.unpinPage(currentDataPageId, false/*Rdisk*/);
    } catch (Exception ex) {
      System.out.println("Exception in getTupleFromPosition() - unpin");
      ex.printStackTrace();
    }
    return nextTuple;
  }

  public static int getPositionFromRID(RID rid, Heapfile hf)
      throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
    boolean flag = true;
    PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
    HFPage currentDirPage = new HFPage();

    int currPosition = 0;
    PageId nextDirPageId = new PageId(0);
    DataPageInfo dpinfo = new DataPageInfo();
    while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
      hf.pinPage(currentDirPageId, currentDirPage, false);
      RID recid = new RID();
      Tuple atuple;
      for (recid = currentDirPage.firstRecord();
          recid != null;  // rid==NULL means no more record
          recid = currentDirPage.nextRecord(recid)) {
        atuple = currentDirPage.getRecord(recid);
        dpinfo = new DataPageInfo(atuple);

        if (rid.pageNo.pid == dpinfo.getPageId().pid) {
          //currPosition += rid.slotNo+1;
          flag = false;
          break;
        } else {
          currPosition += dpinfo.recct;
        }
      }
      // ASSERTIONS: no more record
      // - we have read all datapage records on
      //   the current directory page.

      if (flag) {
        nextDirPageId = currentDirPage.getNextPage();
        hf.unpinPage(currentDirPageId, false /*undirty*/);
        currentDirPageId.pid = nextDirPageId.pid;
      }
    }

    HFPage currentDataPage = new HFPage();
    PageId currentDataPageId = new PageId(dpinfo.getPageId().pid);
    hf.pinPage(currentDataPageId, currentDataPage, false/*Rdisk*/);

    RID record = new RID();
    for (record = currentDataPage.firstRecord();
        !record.equals(rid);  // rid==NULL means no more record
        record = currentDataPage.nextRecord(record)) {
      currPosition += 1;
    }
    hf.unpinPage(currentDataPageId, false);

    return currPosition + 1; //of first record
  }


  public static String getDeleteFileName(String relationName) {
    return relationName + ".del";
  }


  public static Iterator openSortedScanOnDeleteHeap(String fileName)
      throws InvalidRelation, TupleUtilsException, FileScanException, IOException, HFDiskMgrException, HFBufMgrException, HFException, InvalidTupleSizeException, InvalidSlotNumberException, SortException, SortException {

    AttrType[] attrType = new AttrType[1];
    attrType[0] = new AttrType(AttrType.attrInteger);

    short[] attrSize = new short[0];
    //attrSize[0] = 10;
    //attrSize[1] = 10;

    TupleOrder[] order = new TupleOrder[2];
    order[0] = new TupleOrder(TupleOrder.Ascending);
    order[1] = new TupleOrder(TupleOrder.Descending);

    // create an iterator by open a file scan
    FldSpec[] projlist = new FldSpec[1];
    RelSpec rel = new RelSpec(RelSpec.outer);
    projlist[0] = new FldSpec(rel, 1);

    FileScan fscan = null;

    Heapfile file = new Heapfile(fileName);

    fscan = new FileScan(fileName, attrType, attrSize, (short) 1, 1, projlist, null);

    Sort sort;
    sort = new Sort(attrType, (short) 1, attrSize, fscan, 1, order[0], 10, 4);

    return sort;
  }


  public static Tuple getRowTupleFromPosition(int rowPosition, Columnarfile cf, int[] selectedCols,
      AttrType[] reqAttrType, short[] strSizes)
      throws IOException, FieldNumberOutOfBoundException, InvalidSlotNumberException, HFBufMgrException, InvalidTupleSizeException, InvalidTypeException {

    Tuple tuple = new Tuple();
    tuple.setHdr((short) selectedCols.length, reqAttrType, strSizes);

    for (int i = 0; i < selectedCols.length; i++) {
      int indexNumber = selectedCols[i];
      Heapfile heapfile = cf.getColumnFiles()[indexNumber - 1];
      Tuple columnTuple = Util.getTupleFromPosition(rowPosition, heapfile);
      columnTuple.initHeaders();

      ValueClass valueClass = valueClassFactory(reqAttrType[i]);
      valueClass.setValueFromColumnTuple(columnTuple, 1);
      valueClass.setValueinRowTuple(tuple, i + 1);
    }

    return tuple;
  }
}