/*******************************************************************************
 * Copyright (C) 2015  Andrew Chow
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.achow101.bctalkaccountpricer.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class AccountPricer {
	
	// TODO: Change password before compiling
	final String ACCOUNT_NAME = "accountbot";
	final String ACCOUNT_PASS = "NOT THE RIGHT PASSWORD";
	
	int userId = 3;
	
	public AccountPricer(int uid)
	{
		userId = uid;
	}

	public String[] getAccountData() {
		
		// Output stuff
		String[] output = new String[8];
		
		// Summary vars
		String postsString = null;
		String line;
		String username = null;
		String rank = "";
		
		// Retrieve summary page
		try {
			URL url = new URL("https://bitcointalk.org/index.php?action=profile;u=" + userId + ";sa=summary");
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String prevLine = null;
			
			while((line = br.readLine()) != null)
			{
				// Record number of posts
				if(prevLine != null && prevLine.contains("Posts:"))
				{
					int end = line.indexOf("<", 10);
					postsString = line.substring(9, end);
				}
				
				// record username
				if(prevLine != null && prevLine.contains("Name: "))
				{
					int end = line.indexOf("<", 10);
					username = line.substring(9, end);
				}
				
				// record rank
				if(prevLine != null && prevLine.contains("Position: "))
				{
					int end = line.indexOf("<", 10);
					rank = line.substring(9, end);
				}
				
				prevLine = line;
			}
			
			is.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// strings to numbers
		int posts = Integer.parseInt(postsString);
		
		// get number of pages
		int pages = posts / 20;
		if((posts + 1) % 20 != 0)
			pages++;
		
		// other vars
		long[] dates = new long[posts];
		int postcount = 0;
		int goodPosts = 0;
		List<Section> postsSections = new ArrayList<Section>();
		
		// get posts from each page of 20 posts
		for(int i = 0; i < pages; i++)
		{
			// get page
			try {
				URL url = new URL("https://bitcointalk.org/index.php?action=profile;u=" + userId + ";sa=showPosts;start=" + (i * 20));
				InputStream is = url.openStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				
				while((line = br.readLine()) != null)
				{					
					// get date of post
					SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy, hh:mm:ss a");
					Date date;
					
					if(line.startsWith("\t\t\t\t\t\t\t\ton:"))
					{
						String dateStr = line.substring(12);
						if(dateStr.contains("Today"))
						{
							date = new Date();
							String currentDateStr = fmt.format(date);
							dateStr = dateStr.replace("<b>Today</b> at", currentDateStr.substring(0, currentDateStr.lastIndexOf(",") + 1));
						}
						date = fmt.parse(dateStr);
						long unixtime = date.getTime() / 1000;
						dates[postcount] = unixtime;
						
					}
					
					// get nonquoted text of post
					if(line.contains("class=\"post\""))
					{
						postcount++;
						int startIndex = line.indexOf(">");
						if(line.contains("class=\"quote"))
						{
							startIndex = line.lastIndexOf("</div>", line.lastIndexOf("</div>") - 1);
						}
						
						String post = line.substring(startIndex, line.lastIndexOf("</div>"));
						
						if(post.length() >= 75)
							goodPosts++;					
					}
					
					// Get section of post
					else if(line.contains("board="))
					{
						int lastSlashIndex = line.lastIndexOf("/", line.length() - 5);
						int endIndex = line.lastIndexOf("</", lastSlashIndex);
						int startIndex = line.lastIndexOf(">", endIndex);
						String sectionName = line.substring(startIndex, endIndex);
						boolean sectionExists = false;
						int sectionIndex = -1;
						for(int j = 0; j <postsSections.size(); j++)
						{
							if(sectionName.equals(postsSections.get(i).getName()))
							{
								sectionExists = true;
								sectionIndex = j;
							}
						}
						if(postsSections.size() == 0 || !sectionExists)
						{
							postsSections.add(new Section(sectionName));
						}
						else
						{
							postsSections.get(sectionIndex).incrementPostCount();
						}
					}
				}
				
				is.close();
				
				// wait so ip is not banned.
				Thread.sleep(1050);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Get the sections and count up the number of posts in each
		
		// Calculate potential activity, activity, and number of posts in each two week period
		int potActivity = 1;
		long cur2week = 0;
		long prev2week = 0;
		int postsInWeek = 0;
		int activity = 0;
		List<ActivityDetail> activityDetail = new ArrayList<ActivityDetail>();
		for(int i = dates.length - 1; i >= 0; i--)
		{
			cur2week = dates[i] - (dates[i] % 1210000);
			if(cur2week != prev2week)
			{
				potActivity += 14;
				activityDetail.add(new ActivityDetail(prev2week, cur2week, postsInWeek));
				postsInWeek = 0;
			}
			
			if(postsInWeek <= 14)
			{
				activity++;
			}
			
			prev2week = cur2week;
			postsInWeek++;
		}
		
		// Add most recent 2 week period
		activityDetail.add(new ActivityDetail(prev2week, -1, postsInWeek));
		
		// Remove extraneious first 2 week period
		activityDetail.remove(0);
		
		// Put detailed activity info into a string array
		String[] activityBreakdown = new String[activityDetail.size()];
		for(int i = 0; i < activityDetail.size(); i++)
		{
			activityBreakdown[i] = activityDetail.get(i).toString();
		}
		
		// Remove initial 1 so that potential activity is correct.
		potActivity--;
		
		// Get post quality
		double postRatio = (double)goodPosts / posts;
		String postQuality = null;
		
		// Excellent
		if(postRatio >= 0.90)
		{
			postQuality = "Excellent";
		}
		
		// Good
		else if(postRatio >= 0.80)
		{
			postQuality = "Good";
		}
		
		// Fair
		else if(postRatio >= 0.65)
		{
			postQuality = "Fair";
		}
		
		// Poor
		else if(postRatio >= 0.50)
		{
			postQuality = "Poor";
		}
		
		// Very Poor
		else if(postRatio < 0.50)
		{
			postQuality = "Very Poor";
		}
		
		// get trust rating
		int trustScore = checkForTrust();
		String trust = null;
		
		switch(trustScore)
		{
			case -1: trust = "Negative, Red Trust";
				break;
			case 0: trust = "Neutral";
				break;
			case 1: trust = "Positive, Light Green Trust";
				break;
			case 2: trust = "Positive, Dark Green Trust";
				break;
		}
		
		// Get price
		double price = estimatePrice(activity, potActivity, postRatio, trustScore);
		
		// Format price
		DecimalFormat dfmt = new DecimalFormat("##,###,##0.00000000");
		
		// Get ranks
		String potRank = getRank(activity, true);
		if(!rank.equals("Legendary"))
		{
			rank = getRank(activity, false);
		}
		
		// Write to intial output
		output[0] = "User Id: " + userId;
		output[1] = "Name: " + username;
		output[2] = "Posts: " + postcount;
		output[3] = "Activity: " + activity + "(" + rank + ")";
		output[4] = "Potential Activity: " + potActivity + "(Potential " + potRank + ")";
		output[5] = "Post Quality: " + postQuality;
		output[6] = "Trust: " + trust;
		output[7] = "Estimated Price: " + dfmt.format(price);
		
		// Combine output with activity breakdown
		output = combineArrays(output, activityBreakdown);
		
		return output;
	}
	
	private double estimatePrice(int activity, int potentialActivity, double ratio, int trust)
	{
		double price = 0.0006 * activity;
		
		// Extra potential activity
		int epa = potentialActivity - activity;
		price += epa * 0.0003;
		
		// Post quality multipliers
		
		// Excellent
		if(ratio >= 0.90)
		{
			price = price * 1.05;
		}
		
		// Good
		else if(ratio >= 0.80)
		{
			price = price * 1.025;
		}
		
		// Fair
		else if(ratio >= 0.65)
		{
			price = price * 1.00;
		}
		
		// Poor
		else if(ratio >= 0.50)
		{
			price = price * 0.975;
		}
		
		// Very Poor
		else if(ratio < 0.50)
		{
			price = price * 0.95;
		}
		
		// Trust Multipliers		
		switch(trust)
		{
			// Neg trust
			case -1: price = price * 0.15;
				break;
			// Neutral trust
			case 0: price = price * 1.00;
				break;
			// positive light green
			case 1: price = price * 1.10;
				break;
			//positive dark green
			case 2: price = price * 1.20;
				break;
		}
		
		return price;
	}
	
	private int checkForTrust()
	{
		try{
			// Get initial web page
			final WebClient webClient = new WebClient();
			CookieManager cookieMan = new CookieManager();
			cookieMan = webClient.getCookieManager();
			cookieMan.setCookiesEnabled(true);
			final HtmlPage loginPage = webClient.getPage("https://bitcointalk.org/index.php");
			final HtmlForm form = (HtmlForm) loginPage.getFirstByXPath("//form[@action='https://bitcointalk.org/index.php?action=login2']");
	
			// login as accountbot
		    final HtmlSubmitInput button = (HtmlSubmitInput) form.getInputsByValue("Login").get(0);
		    final HtmlTextInput textField = form.getInputByName("user");
		    textField.setValueAttribute(ACCOUNT_NAME);
		    final HtmlPasswordInput textField2 = form.getInputByName("passwrd");
		    textField2.setValueAttribute(ACCOUNT_PASS);
		    final HtmlPage postLoginPage = button.click();
		    
		    // Get profile page
		    final HtmlPage profilePage = webClient.getPage("https://bitcointalk.org/index.php?action=profile;u=" + userId);
		    
		    // Get trust rating
		    final HtmlSpan trustSpan = (HtmlSpan)profilePage.getFirstByXPath("/html/body/div/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td//span[@class='trustscore']");
		    
		    String trustString = trustSpan.toString();
		    
		    // Neg trust
		    if(trustString.contains("color:#DC143C"))
		    {
		    	return -1;
		    }
		    
		    // light green trust
		    if(trustString.contains("color:#74C365"))
		    {
		    	return 1;
		    }
		    
		    // Dark green trust
		    if(trustString.contains("color:#008000"))
		    {
		    	return 2;
		    }
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	    
		return 0;
	}
	
	private String getRank(int activity, boolean potential)
	{
		String rank = "";
		if(activity < 30)
		{
			rank = "Newbie";
		}
		else if(activity >=30 && activity < 60)
		{
			rank = "Jr. Member";
		}
		else if(activity >= 60 && activity < 120)
		{
			rank = "Member";
		}
		else if(activity >= 120 && activity < 240)
		{
			rank = "Full Member";
		}
		else if(activity >= 240 && activity < 480)
		{
			rank = "Sr. Member";
		}
		else if(activity >= 480 && activity < 775)
		{
			rank = "Hero Member";
		}
		else if(activity >= 775)
		{
			rank = "Legendary";
			if(potential)
			{
				rank = "Legendary *Note: Not guaranteed; The account can become Legendary anywhere between 775 and 1030";
			}
		}
		return rank;
	}
	
	private String[] combineArrays(String[] a, String[] b){
		int length = a.length + b.length;
		String[] result = new String[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
	
	private class Section
	{
		private String name;
		private int numPosts = 0;
		
		public Section(String name)
		{
			this.name = name;
		}
		
		public void incrementPostCount()
		{
			numPosts++;
		}
		
		public String getName()
		{
			return name;
		}
		
		public String toString()
		{
			return name + ": " + numPosts + " Posts";
		}
	}
	
	private class ActivityDetail
	{
		private long startTimestamp;
		private long endTimestamp;
		private int numPosts;
		
		public ActivityDetail(long startTimestamp, long endTimestamp, int numPosts)
		{
			this.startTimestamp = startTimestamp;
			this.endTimestamp = endTimestamp;
			this.numPosts = numPosts;
		}
		
		public String toString()
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d yyyy HH:mm:ss"); 
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			// Convert start timestamp to date
			Date startDate = new Date(1000L*startTimestamp);
			String formattedStartDate = sdf.format(startDate);
			
			// Convert end timestamp to date
			String formattedEndDate = "";
			if(endTimestamp == -1)
			{
				formattedEndDate = "Present";
			}
			else
			{
				Date endDate = new Date(1000L*endTimestamp);
				formattedEndDate = sdf.format(endDate);
			}
			
			// Add to array
			return formattedStartDate + " - " + formattedEndDate + ": " + numPosts + " Posts";
		
		}
	}
}
