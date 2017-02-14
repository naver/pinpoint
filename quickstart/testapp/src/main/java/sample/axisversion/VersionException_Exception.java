//
// Generated By:JAX-WS RI IBM 2.1.6 in JDK 6 (JAXB RI IBM JAXB 2.1.10 in JDK 6)
//


package sample.axisversion;

import javax.xml.ws.WebFault;

@WebFault(name = "VersionException", targetNamespace = "http://axisversion.sample")
public class VersionException_Exception
    extends java.lang.Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private VersionException faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public VersionException_Exception(String message, VersionException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param message
     * @param cause
     */
    public VersionException_Exception(String message, VersionException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: sample.axisversion.VersionException
     */
    public VersionException getFaultInfo() {
        return faultInfo;
    }

}
