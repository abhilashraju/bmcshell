package com.ibm.bmcshell;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
public class Introspectables {


/** 1‑st level – the whole service description **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public static class ServiceDescription {
    @JsonProperty("interfaces")
    private List<Interface> interfaces;

    // ----- boilerplate ----------------------------------------------------
    public ServiceDescription() { }                    // Jackson needs a no‑arg ctor
    public ServiceDescription(List<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    public List<Interface> getInterfaces() { return interfaces; }
    public void setInterfaces(List<Interface> interfaces) { this.interfaces = interfaces; }

    @Override
    public String toString() {
        return "ServiceDescription{interfaces=" + interfaces + '}';
    }
}

/** One D‑Bus interface (e.g. org.freedesktop.DBus.Introspectable) **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public static class Interface {
    @JsonProperty("name")
    private String name;

    @JsonProperty("members")
    private List<Member> members;

    public Interface() { }
    public Interface(String name, List<Member> members) {
        this.name = name;
        this.members = members;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Member> getMembers() { return members; }
    public void setMembers(List<Member> members) { this.members = members; }

    @Override
    public String toString() {
        return "Interface{name='" + name + "', members=" + members + '}';
    }
}

/** A member of an interface – can be method, signal or property **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public static class Member {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;        // method / signal / property

    @JsonProperty("signature")
    private String signature;   // may be null

    @JsonProperty("result_value")
    private String resultValue; // may be null

    @JsonProperty("flags")
    private List<String> flags; // may be null

    public Member() { }
    public Member(String name, String type, String signature,
                  String resultValue, List<String> flags) {
        this.name = name;
        this.type = type;
        this.signature = signature;
        this.resultValue = resultValue;
        this.flags = flags;
    }

    // getters / setters ----------------------------------------------------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }

    public List<String> getFlags() { return flags; }
    public void setFlags(List<String> flags) { this.flags = flags; }

    @Override
    public String toString() {
        return "Member{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", signature='" + signature + '\'' +
                ", resultValue='" + resultValue + '\'' +
                ", flags=" + flags +
                '}';
    }
}

}
