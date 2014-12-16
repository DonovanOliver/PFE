import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateUrlStr {
	private String request;

	public GenerateUrlStr(String request) {
		this.request = request;
	}

	public String getHost() {
		String res = "";
		Pattern pattern = Pattern.compile("((Host:)(\\s\\w.*))");
		Matcher matcher = pattern.matcher(this.request);
		if (matcher.find()) {
			res = matcher.group(3);
		}
		return res.replaceAll(" ", "");
	}

	public String getPath() {
		String res = "";
		Pattern pattern = Pattern.compile("((GET)(\\s[^\\s]*))");
		Matcher matcher = pattern.matcher(this.request);
		if (matcher.find()) {
			res = matcher.group(3);
		}
		return res.replaceAll(" ", "");
	}

}
