package com.ibm.bmcshell;

import java.io.PrintStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.shell.standard.commands.Script;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class BmcshellApplication {

	public static class CircularBuffer extends java.io.ByteArrayOutputStream {
		private final int bufferSize;

		public CircularBuffer(int bufferSize) {
			super(bufferSize);
			this.bufferSize = bufferSize;
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			if (count + len > bufferSize) {
				int overflow = (count + len) - bufferSize;
				System.arraycopy(buf, overflow, buf, 0, count - overflow);
				count -= overflow;
			}
			super.write(b, off, len);
		}

		@Override
		public synchronized void write(int b) {
			if (count == bufferSize) {
				System.arraycopy(buf, 1, buf, 0, bufferSize - 1);
				count--;
			}
			super.write(b);
		}

		String getContent() {
			return new String(buf, 0, count);
		}
	}

	final static int BUFFER_SIZE = 8192;

	static final CircularBuffer circularBuffer = new CircularBuffer(BUFFER_SIZE);
	static java.io.PrintStream circularPrintStream = new java.io.PrintStream(circularBuffer, true);

	static String getCircularBufferContent() {
		return circularBuffer.getContent();
	}

	static void clear_buffer() {
		circularBuffer.reset();
		System.out.flush();
		circularPrintStream.flush();
		System.err.flush();
	}

	public static void main(String[] args) {
		// String[] disabledCommands = {"--spring.shell.command.help.enabled=false"};
		// String[] fullArgs = StringUtils.concatenateStringArrays(args,
		// disabledCommands);

		// Create a circular buffer for capturing output

		// Create a PrintStream that writes to both the circular buffer and System.err
		PrintStream dualStream = new PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
				circularPrintStream.write(b);
				System.err.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) {
				circularPrintStream.write(b, off, len);
				System.err.write(b, off, len);
			}
		}, true);

		// Redirect System.out to dualStream
		System.setOut(dualStream);
		// System.setErr(dualStream);
		SpringApplication.run(BmcshellApplication.class, args);
		System.out.println("Exiting....");
		System.exit(0);
	}

	@Order(value = 1)
	static class CustomExceptionResolver implements CommandExceptionResolver {
		int DEFAULT_PRECEDENCE = 1;
		@Autowired
		Script script;

		@Override
		public CommandHandlingResult resolve(Exception e) {
			if (e instanceof CommandExceptionResolver) {
				var ex = (CommandExceptionResolver) e;
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
