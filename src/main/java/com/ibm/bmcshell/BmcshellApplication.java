package com.ibm.bmcshell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.shell.standard.commands.Script;
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
		System.exit(0);
	}

	@Order(value=1)
	static class CustomExceptionResolver implements CommandExceptionResolver {
		int DEFAULT_PRECEDENCE=1;
		@Autowired
		Script script;
		@Override
		public CommandHandlingResult resolve(Exception e) {
			if (e instanceof CommandExceptionResolver) {
				var ex=(CommandExceptionResolver)e;
				return CommandHandlingResult.of("Hi, handled exception\n", 42);
			}
			return null;
		}
	}
	@Bean
	CustomExceptionResolver customExceptionResolver() {
		return new CustomExceptionResolver();
	}



}
