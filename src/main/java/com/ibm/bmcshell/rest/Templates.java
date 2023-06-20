package com.ibm.bmcshell.rest;

public class Templates {
    final static String jsonTemplate="<!DOCTYPE html>\n" +
            "<script>\n" +
            "    function myFunction(){\n" +
            "      var data = JSON.parse('%s');\n" +
            "      document.body.appendChild(traverse(data,process));\n" +
            "    }\n" +
            "    function process(root,key,value) {\n" +
            "        var t = typeof(value);\n" +
            "        if(t ==\"object\"){\n" +
            "            var isArry= Array.isArray(value);\n" +
            "            var item =document.createElement('li');\n" +
            "            if(isArry){\n" +
            "                item.innerHTML = key+\":\";\n" +
            "            }else{\n" +
            "                item.innerHTML = key+\":\";\n" +
            "            }\n" +
            "            \n" +
            "            return item;\n" +
            "        }\n" +
            "        if((t==\"integer\" || t==\"string\"\n" +
            "            ||t==\"double\"||t==\"boolean\")){\n" +
            "                    var item =document.createElement('li');\n" +
            "                    item.innerHTML = key+\" : \"+ value;\n" +
            "                    return item;\n" +
            "        }\n" +
            "        return root;\n" +
            "    }\n" +
            "\n" +
            "    function traverse(o,func) {\n" +
            "        var root =document.createElement('ul');\n" +
            "        root.setAttribute(\"class\",\"a\");\n" +
            "        for (var key in o) {\n" +
            "            var itemroot =document.createElement('ul');\n" +
            "            itemroot.setAttribute(\"class\",\"b\");\n" +
            "            itemroot =func.apply(this,[itemroot,key,o[key]]);  \n" +
            "\n" +
            "            if (o[key] !== null && typeof(o[key])==\"object\") {\n" +
            "                //going one step down in the object tree!!\n" +
            "                itemroot.appendChild(traverse(o[key],func));\n" +
            "            }\n" +
            "            root.appendChild(itemroot);\n" +
            "        }\n" +
            "        return root;\n" +
            "    }\n" +
            "\n" +
            "  </script>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "\n" +
            "        <style>\n" +
            "        ul.a {\n" +
            "          list-style-type: none;\n" +
            "        }\n" +
            "        \n" +
            "        ul.b {\n" +
            "          list-style-type: none;\n" +
            "        }\n" +
            "        \n" +
            "        ol.c {\n" +
            "          list-style-type: upper-roman;\n" +
            "        }\n" +
            "        \n" +
            "        ol.d {\n" +
            "          list-style-type: lower-alpha;\n" +
            "        }\n" +
            "        </style>\n" +
            "\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <title>JavaScript - Display JSON data as list</title>\n" +
            "</head>\n" +
            "<body onload=\"myFunction()\">\n" +
            "  <h1>%s</h1>\n" +
            "</body>\n" +
            "</html>\n" +
            "\n" +
            "<code> </code>\n" +
            "\n" +
            "\n";
}
