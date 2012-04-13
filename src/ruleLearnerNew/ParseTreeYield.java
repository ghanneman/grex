package ruleLearnerNew;

import java.util.List;
import java.util.ArrayList;
//import java.util.Set;
//import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * @author ghannema
 */
public class ParseTreeYield
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public ParseTreeYield()
	{
		words = new ArrayList<String>();
	}


	public ParseTreeYield(String parenString)
	{
		// Break the string into a node label and its yield or subtree:
		words = new ArrayList<String>();
		parenString = parenString.trim();
		Matcher m = terminalString.matcher(parenString);
		while(m.find())
		{
			words.add(m.group(1));
		}
	}

	
	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public List<String> getYield(int startIndex, int endIndex)
	{
		return words.subList(startIndex, endIndex + 1);
		/*
		String result = "";
		for(int i = startIndex; i <= endIndex; i++)
			result += (words.get(i) + " ");
		return result.trim();
		*/
	}


	// PUBLIC MEMBER ATTRIBUTES: ////////////////////////////////////////////

	public static final Pattern terminalString =
		Pattern.compile("\\([^() ]* ([^()]*)\\)");


	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	
	private List<String> words;
}
