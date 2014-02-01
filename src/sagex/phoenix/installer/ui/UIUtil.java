package sagex.phoenix.installer.ui;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.google.common.base.Joiner;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

public class UIUtil {

	public UIUtil() {
	}
	
	public static void add(Container c, JComponent comp, int col, int row, String colAlign, String rowAlign) {
		Joiner j = Joiner.on(",");
		String contstaint = j.join(col, row, colAlign, rowAlign);
		c.add(comp, contstaint);
	}

	public static void add(Container c, JComponent comp, int col, int row, int colSpan, int rowSpan, String colAlign,
			String rowAlign) {
		Joiner j = Joiner.on(",");
		String contstaint = j.join(col, row, colSpan, rowSpan, colAlign, rowAlign);
		c.add(comp, contstaint);
	}
	
	public static ColumnSpec[] columns(ColumnSpec... spec) {
		List<ColumnSpec> arr = new ArrayList<ColumnSpec>();
		arr.add(FormFactory.RELATED_GAP_COLSPEC);
		for (ColumnSpec c: spec) {
			arr.add(c);
			arr.add(FormFactory.RELATED_GAP_COLSPEC);
		}
		return arr.toArray(new ColumnSpec[] {});
	}

	public static RowSpec[] rows(RowSpec... spec) {
		List<RowSpec> arr = new ArrayList<RowSpec>();
		arr.add(FormFactory.RELATED_GAP_ROWSPEC);
		for (RowSpec c: spec) {
			arr.add(c);
			arr.add(FormFactory.RELATED_GAP_ROWSPEC);
		}
		return arr.toArray(new RowSpec[] {});
	}
}
