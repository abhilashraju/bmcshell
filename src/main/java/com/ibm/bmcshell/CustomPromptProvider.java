package com.ibm.bmcshell;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class CustomPromptProvider implements PromptProvider
{
    public static class ShellData
    {
        String prompt="Not Logged In";
        AttributedStyle style=AttributedStyle.DEFAULT.background(AttributedStyle.RED);
        public ShellData(String p, int color)
        {
            prompt=p;
            style = AttributedStyle.DEFAULT.background(color);
        }
        ShellData(){}

    }
    ShellData shellData=new ShellData();
    @Override
    public AttributedString getPrompt()
    {
        return new AttributedString(
                "BMC SHELL(" +shellData.prompt+ ")==> ",shellData.style);
    }

    public void setShellData(ShellData shellData) {
        this.shellData = shellData;
    }
}
