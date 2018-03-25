package columnar;

import static global.AttrType.attrInteger;
import static global.AttrType.attrNull;
import static global.AttrType.attrReal;
import static global.AttrType.attrString;
import static global.AttrType.attrSymbol;

import diskmgr.Page;
import global.AttrType;
import global.Convert;
import global.PageId;
import global.RID;
import heap.*;

import java.io.IOException;

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

  public static String getBitAsString(byte dataByte, int position)
  {
    String s1 = String.format("%8s", Integer.toBinaryString(dataByte & 0xFF)).replace(' ', '0');
    return  new String(""+s1.charAt(position));
  }

  public static void printBitsInByte(byte[] val)
  {
    for(int i =0 ; i<val.length; i++)
    {
      byte b1 = val[i];
      String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
      System.out.println(s1); // 10000001

      //System.out.println(Integer.toBinaryString( (int) val[i]));
    }
  }

    public static RID getRIDFromPosition(int position, Heapfile hf) throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
        int curcount = position;
        PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
        HFPage currentDirPage = new HFPage();
        PageId nextDirPageId = new PageId(0);

        Page pageinbuffer = new Page();

        boolean flag = true;

        RID recid = new RID();
        while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
            hf.pinPage(currentDirPageId, currentDirPage, false);

            Tuple atuple;
            for (recid = currentDirPage.firstRecord();
                 recid != null;  // rid==NULL means no more record
                 recid = currentDirPage.nextRecord(recid)) {
                atuple = currentDirPage.getRecord(recid);
                DataPageInfo dpinfo = new DataPageInfo(atuple);

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
        //recid points to data page with the position

        HFPage currentDataPage = new HFPage();
        PageId currentDataPageId = new PageId(recid.pageNo.pid);
        hf.pinPage(currentDirPageId, currentDirPage, false/*Rdisk*/);

        RID record = currentDataPage.firstRecord();
        curcount--;
        while( record != null && curcount>0) {
            record = currentDataPage.nextRecord(record);
            curcount--;
        }
        return record;
    }


  public static Tuple getTupleFromPosition(int position, Heapfile hf) throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
    int curcount = position;
    PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
    HFPage currentDirPage = new HFPage();
    PageId nextDirPageId = new PageId(0);

    Page pageinbuffer = new Page();

    boolean flag = true;

    while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
      hf.pinPage(currentDirPageId, currentDirPage, false);

      RID recid = new RID();
      Tuple atuple;
      for (recid = currentDirPage.firstRecord();
          recid != null;  // rid==NULL means no more record
          recid = currentDirPage.nextRecord(recid)) {
        atuple = currentDirPage.getRecord(recid);
        DataPageInfo dpinfo = new DataPageInfo(atuple);

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

    RID cur = currentDirPage.firstRecord();
    Scan sc = new Scan(hf);
    Tuple nextTuple = new Tuple();
    while (nextTuple != null && curcount >= 0) {
      nextTuple = sc.getNext(cur);
      curcount--;
    }
    return nextTuple;
  }

    public static int getPositionFromRID(RID rid, Heapfile hf) throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
        boolean flag = true;
        PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
        HFPage currentDirPage = new HFPage();

        int currPosition = 0;
        PageId nextDirPageId = new PageId(0);

        while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
            hf.pinPage(currentDirPageId, currentDirPage, false);
            RID recid = new RID();
            Tuple atuple;
            for (recid = currentDirPage.firstRecord();
                 recid != null;  // rid==NULL means no more record
                 recid = currentDirPage.nextRecord(recid)) {
                atuple = currentDirPage.getRecord(recid);
                DataPageInfo dpinfo = new DataPageInfo(atuple);

                if (rid.pageNo.pid == dpinfo.getPageId().pid) {
                    currPosition += rid.slotNo;
                    flag = false;
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
        return currPosition;
    }
}