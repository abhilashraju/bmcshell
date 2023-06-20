package com.ibm.bmcshell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class BmcshellApplication {

	@RequestMapping("/hello")
	String home() {
		return "Hello World!";
	}
	public static void main(String[] args) {
//		String[] disabledCommands = {"--spring.shell.command.help.enabled=false"};
//		String[] fullArgs = StringUtils.concatenateStringArrays(args, disabledCommands);

		SpringApplication.run(BmcshellApplication.class, args);
		System.out.println("Exiting....");
	}


}
