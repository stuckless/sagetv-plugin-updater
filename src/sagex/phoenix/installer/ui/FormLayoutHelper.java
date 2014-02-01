package sagex.phoenix.installer.ui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class FormLayoutHelper {
	private EventBus bus;
	
	private List<ColumnSpec> columns = new ArrayList<ColumnSpec>();
	private List<RowSpec> rows = new ArrayList<RowSpec>();
	private Map<String, Integer> colMap = new HashMap<String, Integer>();
	private Map<String, Integer> rowMap = new HashMap<String, Integer>();
	private Container container;
	
	private Map<String, JComponent> components= new HashMap<String, JComponent>();
	
	public FormLayoutHelper(EventBus bus, Container container) {
		this.container = container;
		columns.add(FormFactory.RELATED_GAP_COLSPEC);
		rows.add(FormFactory.RELATED_GAP_ROWSPEC);
		this.bus=bus;
		bus.register(this);
	}

	public void addCol(String id, ColumnSpec col) {
		columns.add(col);
		colMap.put(id, columns.size());
		columns.add(FormFactory.RELATED_GAP_COLSPEC);
	}

	public void addRow(String id, RowSpec col) {
		rows.add(col);
		rowMap.put(id, rows.size());
		rows.add(FormFactory.RELATED_GAP_ROWSPEC);
	}

	public void add(String id, String col, String row, String colAlign, String rowAlign, JComponent comp) {
		add(col, row, colAlign, rowAlign, comp);
		comp.setName(id);
		components.put(id, comp);
	}

	public void add(String id, String col, String row, String colAlign, String rowAlign, JButton comp) {
		add(col, row, colAlign, rowAlign, comp);
		components.put(id, comp);
		comp.setName(id);
		comp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				bus.post(e);
			}
		});
	}

	public void add(String col, String row, String colAlign, String rowAlign, JComponent comp) {
		if (container.getLayout()==null|| !(container.getLayout() instanceof FormLayout)) {
			setLayout();
		}
		UIUtil.add(container, comp, col(col), row(row), colAlign, rowAlign);
	}

	public void add(String id, String col, String row, int colSpan, int rowSpan, String colAlign, String rowAlign, JComponent comp) {
		add(col, row, colSpan, rowSpan, colAlign, rowAlign, comp);
		comp.setName(id);
		components.put(id, comp);
	}

	public void add(String col, String row, int colSpan, int rowSpan, String colAlign, String rowAlign, JComponent comp) {
		if (container.getLayout()==null|| !(container.getLayout() instanceof FormLayout)) {
			setLayout();
		}
		UIUtil.add(container, comp, col(col), row(row), colSpan, rowSpan, colAlign, rowAlign);
	}
	
	public ColumnSpec[] columns() {
		return columns.toArray(new ColumnSpec[]{});
	}
	
	public RowSpec[] rows() {
		return rows.toArray(new RowSpec[]{});
	}
	
	public int row(String id) {
		return rowMap.get(id);
	}
	
	public int col(String id) {
		return colMap.get(id);
	}
	
	public void setLayout() {
		container.setLayout(new FormLayout(columns(), rows()));
	}

	public int maxColSpan() {
		return columns.size()-2;
	}
	
	public void manage(String id, JComponent comp) {
		components.put(id, comp);
	}
	
	public <V> V getValue(String id) {
		JComponent comp = components.get(id);
		if (comp instanceof JTextComponent) {
			return (V) ((JTextComponent)comp).getText();
		} else if (comp instanceof JLabel) {
			return (V) ((JLabel)comp).getText();
		} else if (comp instanceof JComboBox) {
			return (V) ((JComboBox)comp).getSelectedItem();
		} else if (comp instanceof JCheckBox) {
			return (V)(Boolean) ((JCheckBox)comp).isSelected();
		}
		throw new UnsupportedOperationException("not implemented for " + comp);
	}
	
	public <V> void setValue(String id, V value) {
		JComponent comp = components.get(id);
		if (comp instanceof JTextComponent) {
			((JTextComponent)comp).setText((String) value);
		} else if (comp instanceof JLabel) {
			((JLabel)comp).setText((String) value);
		} else if (comp instanceof JComboBox) {
			((JComboBox)comp).setSelectedItem(value);
		} else {
			throw new UnsupportedOperationException("not implemented for " + comp);
		}
		
		bus.post(new ValueUpdatedEvent(id, value));
	}
	
	@Subscribe
	public void onUpdateValue(UpdateValueEvent evt) {
		JComponent c = components.get(evt.id);
		if (c!=null) {
			setValue(evt.id, evt.value);
		}
	}

	public static void postUpdate(final EventBus bus, final String id, final Object value) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				bus.post(new UpdateValueEvent(id, value));
			}
		});
	}

	public <C> C getComponent(String id) {
		return (C) components.get(id);
	}
	
	public void connectClick(final String id, final ActionListener l) {
		bus.register(new ActionListener() {
			@Subscribe
			public void actionPerformed(ActionEvent e) {
				JComponent c = (JComponent) e.getSource();
				if (id.equalsIgnoreCase(c.getName())) {
					l.actionPerformed(e);
				}
			}
		});
	}

	public void connectUpdate(final String id, final ValueUpdatedListener l) {
		bus.register(new ValueUpdatedListener() {
			@Subscribe
			public void valueUpdated(ValueUpdatedEvent e) {
				if (id.equalsIgnoreCase(e.id)) {
					l.valueUpdated(e);
				}
			}
		});
		
		final JComponent comp = components.get(id);
		if (comp instanceof JTextComponent) {
			((JTextComponent)comp).getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent e) {
					bus.post(new ValueUpdatedEvent(id, ((JTextComponent) comp).getText()));
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					bus.post(new ValueUpdatedEvent(id, ((JTextComponent) comp).getText()));
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					bus.post(new ValueUpdatedEvent(id, ((JTextComponent) comp).getText()));
				}
			});
		}
	}
	
	public Container container() {
		return container;
	}
}
