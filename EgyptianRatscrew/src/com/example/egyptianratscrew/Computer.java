package com.example.egyptianratscrew;

public class Computer
{	
	long speedInMillis;
	
	public Computer(int difficulty)
	{
		if (difficulty == 1) speedInMillis = 1500;
		else if (difficulty == 2) speedInMillis = 1375;
		else if (difficulty == 3) speedInMillis = 1250;
		else if (difficulty == 4) speedInMillis = 1125;
	}
	
	public long getDelay()
	{
		return speedInMillis;
	}
}
