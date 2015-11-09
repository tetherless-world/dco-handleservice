package edu.rpi.twc.dco.dcohandleservice;

import javax.xml.bind.annotation.XmlRootElement;
/**
 * This is the object that encode transction XML files for the restful service. 
 *
 * This class represents a handle object. The handle id (DCO-ID), the
 * type of value that is stored (URL) and the value (URL that the handle
 * resolves to.
 *
 * @author cheny
 *
 */
@XmlRootElement
public class handle {

  private String id;
  private String type;
  private String value;

  public handle(){
	  
  }
  public handle(String id,String type,String value){
	  this.id = id;
	  this.type = type;
	  this.value = value;
  }
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getValue(){
	  return value;
  }
  
  public void setValue(String value){
	  this.value = value;
  }
  
  public void setType(String type){
	  this.type = type;
  }

  public String getType(){
	  return this.type;
  }
}
