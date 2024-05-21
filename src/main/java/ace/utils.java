package ace;
import java.util.*;

class Utils {
    /*
      Check if a string is a float number, reference:
      https://www.baeldung.com/java-check-string-number
    */
    public static boolean isNumeric(String strNum) {
	if (strNum == null) {
	    return false;
	}
	try {
	    double d = Double.parseDouble(strNum);
	} catch (NumberFormatException nfe) {
	    return false;
	}
	return true;
    }

    // make a string that represents the CPT
    public static String make_CPT(List<Double> CPT, List<Integer> cards){
	String res;
	if (cards.size() == 0){
	    res = "( ";
	    for (int i = 0; i < CPT.size(); i ++){
		res += String.valueOf(CPT.get(i));
		res += " ";
	    }
	    res += ")";
	}
	else{
	    int card = cards.get(0);
	    int CPT_size = CPT.size();
	    int chunk_size = CPT_size / card;
	    res = "(";
	    for (int i = 0; i < CPT_size; i += chunk_size){
		//List<Integer> new_cards = (cards.size() > 1 ? cards.subList(1,cards.size()) : (new ArrayList<Integer>()));
		res += make_CPT(CPT.subList(i,i+chunk_size),cards.subList(1,cards.size()));
	    }
	    res += ")";
	}
	return res;
    }

}
