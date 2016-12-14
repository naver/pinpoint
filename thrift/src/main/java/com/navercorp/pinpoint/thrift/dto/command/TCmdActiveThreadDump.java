/**
 * Autogenerated by Thrift Compiler (0.9.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.navercorp.pinpoint.thrift.dto.command;

import org.apache.thrift.EncodingUtils;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.2)", date = "2016-12-15")
public class TCmdActiveThreadDump implements org.apache.thrift.TBase<TCmdActiveThreadDump, TCmdActiveThreadDump._Fields>, java.io.Serializable, Cloneable, Comparable<TCmdActiveThreadDump> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TCmdActiveThreadDump");

  private static final org.apache.thrift.protocol.TField EXEC_TIME_FIELD_DESC = new org.apache.thrift.protocol.TField("execTime", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField TARGET_THREAD_NAME_LIST_FIELD_DESC = new org.apache.thrift.protocol.TField("targetThreadNameList", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TCmdActiveThreadDumpStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TCmdActiveThreadDumpTupleSchemeFactory());
  }

  private long execTime; // optional
  private List<String> targetThreadNameList; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    EXEC_TIME((short)1, "execTime"),
    TARGET_THREAD_NAME_LIST((short)2, "targetThreadNameList");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // EXEC_TIME
          return EXEC_TIME;
        case 2: // TARGET_THREAD_NAME_LIST
          return TARGET_THREAD_NAME_LIST;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __EXECTIME_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.EXEC_TIME,_Fields.TARGET_THREAD_NAME_LIST};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.EXEC_TIME, new org.apache.thrift.meta_data.FieldMetaData("execTime", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.TARGET_THREAD_NAME_LIST, new org.apache.thrift.meta_data.FieldMetaData("targetThreadNameList", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TCmdActiveThreadDump.class, metaDataMap);
  }

  public TCmdActiveThreadDump() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TCmdActiveThreadDump(TCmdActiveThreadDump other) {
    __isset_bitfield = other.__isset_bitfield;
    this.execTime = other.execTime;
    if (other.isSetTargetThreadNameList()) {
      List<String> __this__targetThreadNameList = new ArrayList<String>(other.targetThreadNameList);
      this.targetThreadNameList = __this__targetThreadNameList;
    }
  }

  public TCmdActiveThreadDump deepCopy() {
    return new TCmdActiveThreadDump(this);
  }

  @Override
  public void clear() {
    setExecTimeIsSet(false);
    this.execTime = 0;
    this.targetThreadNameList = null;
  }

  public long getExecTime() {
    return this.execTime;
  }

  public void setExecTime(long execTime) {
    this.execTime = execTime;
    setExecTimeIsSet(true);
  }

  public void unsetExecTime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __EXECTIME_ISSET_ID);
  }

  /** Returns true if field execTime is set (has been assigned a value) and false otherwise */
  public boolean isSetExecTime() {
    return EncodingUtils.testBit(__isset_bitfield, __EXECTIME_ISSET_ID);
  }

  public void setExecTimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __EXECTIME_ISSET_ID, value);
  }

  public int getTargetThreadNameListSize() {
    return (this.targetThreadNameList == null) ? 0 : this.targetThreadNameList.size();
  }

  public java.util.Iterator<String> getTargetThreadNameListIterator() {
    return (this.targetThreadNameList == null) ? null : this.targetThreadNameList.iterator();
  }

  public void addToTargetThreadNameList(String elem) {
    if (this.targetThreadNameList == null) {
      this.targetThreadNameList = new ArrayList<String>();
    }
    this.targetThreadNameList.add(elem);
  }

  public List<String> getTargetThreadNameList() {
    return this.targetThreadNameList;
  }

  public void setTargetThreadNameList(List<String> targetThreadNameList) {
    this.targetThreadNameList = targetThreadNameList;
  }

  public void unsetTargetThreadNameList() {
    this.targetThreadNameList = null;
  }

  /** Returns true if field targetThreadNameList is set (has been assigned a value) and false otherwise */
  public boolean isSetTargetThreadNameList() {
    return this.targetThreadNameList != null;
  }

  public void setTargetThreadNameListIsSet(boolean value) {
    if (!value) {
      this.targetThreadNameList = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case EXEC_TIME:
      if (value == null) {
        unsetExecTime();
      } else {
        setExecTime((Long)value);
      }
      break;

    case TARGET_THREAD_NAME_LIST:
      if (value == null) {
        unsetTargetThreadNameList();
      } else {
        setTargetThreadNameList((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case EXEC_TIME:
      return Long.valueOf(getExecTime());

    case TARGET_THREAD_NAME_LIST:
      return getTargetThreadNameList();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case EXEC_TIME:
      return isSetExecTime();
    case TARGET_THREAD_NAME_LIST:
      return isSetTargetThreadNameList();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TCmdActiveThreadDump)
      return this.equals((TCmdActiveThreadDump)that);
    return false;
  }

  public boolean equals(TCmdActiveThreadDump that) {
    if (that == null)
      return false;

    boolean this_present_execTime = true && this.isSetExecTime();
    boolean that_present_execTime = true && that.isSetExecTime();
    if (this_present_execTime || that_present_execTime) {
      if (!(this_present_execTime && that_present_execTime))
        return false;
      if (this.execTime != that.execTime)
        return false;
    }

    boolean this_present_targetThreadNameList = true && this.isSetTargetThreadNameList();
    boolean that_present_targetThreadNameList = true && that.isSetTargetThreadNameList();
    if (this_present_targetThreadNameList || that_present_targetThreadNameList) {
      if (!(this_present_targetThreadNameList && that_present_targetThreadNameList))
        return false;
      if (!this.targetThreadNameList.equals(that.targetThreadNameList))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_execTime = true && (isSetExecTime());
    list.add(present_execTime);
    if (present_execTime)
      list.add(execTime);

    boolean present_targetThreadNameList = true && (isSetTargetThreadNameList());
    list.add(present_targetThreadNameList);
    if (present_targetThreadNameList)
      list.add(targetThreadNameList);

    return list.hashCode();
  }

  @Override
  public int compareTo(TCmdActiveThreadDump other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetExecTime()).compareTo(other.isSetExecTime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExecTime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.execTime, other.execTime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetTargetThreadNameList()).compareTo(other.isSetTargetThreadNameList());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTargetThreadNameList()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.targetThreadNameList, other.targetThreadNameList);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TCmdActiveThreadDump(");
    boolean first = true;

    if (isSetExecTime()) {
      sb.append("execTime:");
      sb.append(this.execTime);
      first = false;
    }
    if (isSetTargetThreadNameList()) {
      if (!first) sb.append(", ");
      sb.append("targetThreadNameList:");
      if (this.targetThreadNameList == null) {
        sb.append("null");
      } else {
        sb.append(this.targetThreadNameList);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TCmdActiveThreadDumpStandardSchemeFactory implements SchemeFactory {
    public TCmdActiveThreadDumpStandardScheme getScheme() {
      return new TCmdActiveThreadDumpStandardScheme();
    }
  }

  private static class TCmdActiveThreadDumpStandardScheme extends StandardScheme<TCmdActiveThreadDump> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TCmdActiveThreadDump struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // EXEC_TIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.execTime = iprot.readI64();
              struct.setExecTimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // TARGET_THREAD_NAME_LIST
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list40 = iprot.readListBegin();
                struct.targetThreadNameList = new ArrayList<String>(_list40.size);
                String _elem41;
                for (int _i42 = 0; _i42 < _list40.size; ++_i42)
                {
                  _elem41 = iprot.readString();
                  struct.targetThreadNameList.add(_elem41);
                }
                iprot.readListEnd();
              }
              struct.setTargetThreadNameListIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TCmdActiveThreadDump struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.isSetExecTime()) {
        oprot.writeFieldBegin(EXEC_TIME_FIELD_DESC);
        oprot.writeI64(struct.execTime);
        oprot.writeFieldEnd();
      }
      if (struct.targetThreadNameList != null) {
        if (struct.isSetTargetThreadNameList()) {
          oprot.writeFieldBegin(TARGET_THREAD_NAME_LIST_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.targetThreadNameList.size()));
            for (String _iter43 : struct.targetThreadNameList)
            {
              oprot.writeString(_iter43);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TCmdActiveThreadDumpTupleSchemeFactory implements SchemeFactory {
    public TCmdActiveThreadDumpTupleScheme getScheme() {
      return new TCmdActiveThreadDumpTupleScheme();
    }
  }

  private static class TCmdActiveThreadDumpTupleScheme extends TupleScheme<TCmdActiveThreadDump> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TCmdActiveThreadDump struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetExecTime()) {
        optionals.set(0);
      }
      if (struct.isSetTargetThreadNameList()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetExecTime()) {
        oprot.writeI64(struct.execTime);
      }
      if (struct.isSetTargetThreadNameList()) {
        {
          oprot.writeI32(struct.targetThreadNameList.size());
          for (String _iter44 : struct.targetThreadNameList)
          {
            oprot.writeString(_iter44);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TCmdActiveThreadDump struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.execTime = iprot.readI64();
        struct.setExecTimeIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list45 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.targetThreadNameList = new ArrayList<String>(_list45.size);
          String _elem46;
          for (int _i47 = 0; _i47 < _list45.size; ++_i47)
          {
            _elem46 = iprot.readString();
            struct.targetThreadNameList.add(_elem46);
          }
        }
        struct.setTargetThreadNameListIsSet(true);
      }
    }
  }

}

