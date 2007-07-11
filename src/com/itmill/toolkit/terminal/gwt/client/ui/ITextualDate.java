package com.itmill.toolkit.terminal.gwt.client.ui;


import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Widget;
import com.itmill.toolkit.terminal.gwt.client.Client;
import com.itmill.toolkit.terminal.gwt.client.DateLocale;
import com.itmill.toolkit.terminal.gwt.client.Paintable;
import com.itmill.toolkit.terminal.gwt.client.UIDL;
import com.itmill.toolkit.terminal.gwt.gwtwidgets.util.SimpleDateFormat;

public class ITextualDate extends IDateField implements Paintable, ChangeListener {
	
	private ITextField text;
	
	private SimpleDateFormat format;
	
	private DateLocale dl;
	
	public ITextualDate() {
		super();
		text = new ITextField();
		text.addChangeListener(this);
		add(text);
	}
	
	public void updateFromUIDL(UIDL uidl, Client client) {
		super.updateFromUIDL(uidl, client);
		buildTime();
	}

	protected void buildTime() {
		dl = new DateLocale();
		DateLocale.setLocale(currentLocale);
		
		DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_YEAR;
		if(currentResolution == IDateField.RESOLUTION_MONTH)
			DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_MONTH;
		else if(currentResolution >= IDateField.RESOLUTION_DAY)
			DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_DAY;
			
		format = new SimpleDateFormat(verifyFormat(dts.getDateFormat()));
		format.setLocale(dl);
		
		String dateText = format.format(date);
		
		if(currentResolution >= IDateField.RESOLUTION_HOUR) {
			DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_ALL;
			int h = date.getHours();
			if(h > 11 && dts.isTwelveHourClock())
				h -= 12;
			int m = currentResolution > IDateField.RESOLUTION_HOUR? date.getMinutes() : 0;
			dateText += " " + (h<10?"0"+h:""+h) + dts.getClockDelimeter() + (m<10?"0"+m:""+m);
		}
		if(currentResolution >= IDateField.RESOLUTION_SEC) {
			int s = date.getSeconds();
			dateText += dts.getClockDelimeter() + (s<10?"0"+s:""+s);
		}
		if(currentResolution == IDateField.RESOLUTION_MSEC) {
			int ms = getMilliseconds();
			String text = ""+ms;
			if(ms<10) text = "00"+text;
			else if(ms<100) text = "0"+text;
			dateText += "." + text;
		}
		if(currentResolution >= IDateField.RESOLUTION_HOUR && dts.isTwelveHourClock())
			dateText += " " + (date.getHours()<12? dts.getAmPmStrings()[0] : dts.getAmPmStrings()[1]);
		
		text.setText(dateText);
		text.setEnabled(enabled&&!readonly);
		
		if(readonly)
			text.addStyleName("i-readonly");
		else
			text.removeStyleName("i-readonly");
	}

	public void onChange(Widget sender) {
		if(sender == text) {
			DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_ALL;
			if(currentResolution == IDateField.RESOLUTION_YEAR)
				DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_YEAR;
			else if(currentResolution == IDateField.RESOLUTION_MONTH)
				DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_MONTH;
			else if(currentResolution == IDateField.RESOLUTION_DAY)
				DateLocale.SUPPORTED_DF_TOKENS = DateLocale.TOKENS_RESOLUTION_DAY;
			
			String f = verifyFormat(dts.getDateFormat());
			
			if(currentResolution >= IDateField.RESOLUTION_HOUR)
				f += " " + (dts.isTwelveHourClock()?
						DateLocale.TOKEN_HOUR_12 + DateLocale.TOKEN_HOUR_12
					  : DateLocale.TOKEN_HOUR_24 + DateLocale.TOKEN_HOUR_24)
						+ dts.getClockDelimeter() + DateLocale.TOKEN_MINUTE + DateLocale.TOKEN_MINUTE;
			if(currentResolution >= IDateField.RESOLUTION_SEC)
				f += dts.getClockDelimeter() + DateLocale.TOKEN_SECOND + DateLocale.TOKEN_SECOND;
			if(currentResolution == IDateField.RESOLUTION_MSEC)
				f += "." + DateLocale.TOKEN_MILLISECOND + DateLocale.TOKEN_MILLISECOND + DateLocale.TOKEN_MILLISECOND;
			if(currentResolution >= IDateField.RESOLUTION_HOUR && dts.isTwelveHourClock())
				f += " " + DateLocale.TOKEN_AM_PM;
			
			format = new SimpleDateFormat(f);
			DateLocale.setLocale(currentLocale);
			format.setLocale(dl);
			
			try {
				date = format.parse(text.getText());
			} catch (Exception e) {
				// TODO redirect to console
				System.out.println(e);
				text.addStyleName(ITextField.CLASSNAME+"-error");
				Timer t = new Timer() {
					public void run() {
						text.removeStyleName(ITextField.CLASSNAME+"-error");
					}
				};
				t.schedule(2000);
				return;
			}
			
			// Update variables
			// (only the smallest defining resolution needs to be immediate)
			client.updateVariable(id, "year", date.getYear()+1900, currentResolution==IDateField.RESOLUTION_YEAR);
			if(currentResolution >= IDateField.RESOLUTION_MONTH)
				client.updateVariable(id, "month", date.getMonth()+1, currentResolution==IDateField.RESOLUTION_MONTH&&immediate);
			if(currentResolution >= IDateField.RESOLUTION_DAY)
				client.updateVariable(id, "day", date.getDate(), currentResolution==IDateField.RESOLUTION_DAY&&immediate);
			if(currentResolution >= IDateField.RESOLUTION_HOUR)
				client.updateVariable(id, "hour", date.getHours(), currentResolution==IDateField.RESOLUTION_HOUR&&immediate);
			if(currentResolution >= IDateField.RESOLUTION_MIN)
				client.updateVariable(id, "min", date.getMinutes(), currentResolution==IDateField.RESOLUTION_MIN&&immediate);
			if(currentResolution >= IDateField.RESOLUTION_SEC)
				client.updateVariable(id, "sec", date.getSeconds(), currentResolution==IDateField.RESOLUTION_SEC&&immediate);
			if(currentResolution == IDateField.RESOLUTION_MSEC)
				client.updateVariable(id, "msec", getMilliseconds(), immediate);
			
			buildTime();
		}
	}
	
	private String verifyFormat(String format) {
		// Remove unnecessary d & M if resolution is too low
		if(currentResolution < IDateField.RESOLUTION_DAY)
			format = format.replaceAll("d", "");
		if(currentResolution < IDateField.RESOLUTION_MONTH)
			format = format.replaceAll("M", "");
		
		// Remove unsupported patterns
		// TODO support for 'G', era designator (used at least in Japan)
		format = format.replaceAll("[GzZwWkK]", "");
		
		// Remove extra delimiters ('/' and '.')
		while(format.startsWith("/") || format.startsWith(".") || format.startsWith("-"))
			format = format.substring(1);
		while(format.endsWith("/") || format.endsWith(".") || format.endsWith("-"))
			format = format.substring(0, format.length()-1);
		
		// Remove duplicate delimiters
		format = format.replaceAll("//", "/");
		format = format.replaceAll("\\.\\.", ".");
		format = format.replaceAll("--", "-");
			
		return format.trim();
	}
}
