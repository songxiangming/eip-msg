package com.samuel.eipmsg;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor  // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all arguments
@ToString
public class Payment {
	private String id;
	private Double amount;
	private String currency;
	private String description;
	private String debitAcct; // Account number from which the payment is debited
	private String creditAcct; // Account number to which the payment is credited
}