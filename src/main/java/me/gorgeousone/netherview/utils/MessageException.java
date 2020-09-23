package me.gorgeousone.netherview.utils;

import me.gorgeousone.netherview.Message;

import java.util.Arrays;

public class MessageException extends Exception {
	
	private final Message message;
	private final String[] placeholderValues;
	
	public MessageException(Message message, String... placeholderValues) {
		super(Arrays.toString(message.getMessage(placeholderValues)));
		this.message = message;
		this.placeholderValues = placeholderValues;
	}
	
	public Message getPlayerMessage() {
		return message;
	}
	
	public String[] getPlaceholderValues() {
		return placeholderValues;
	}
}