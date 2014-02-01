package sagex.phoenix.installer.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import sagex.phoenix.installer.IOutput;
import sagex.phoenix.installer.PhoenixInstaller;
import sagex.phoenix.installer.PluginManager;
import sagex.phoenix.installer.SageTVPluginModelImpl;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

public class PhoenixInstallerUI2  implements IOutput, ValueUpdatedListener {
	private static final String FIELD_SAGETV_HOME = "sagetv_home";
	private static final String FIELD_PLUGINS = "plugins";
	private static final String FIELD_LOG = "log";
	private static final String FIELD_BUTTON_CHOOSE_SAGETV = "choose_sagetv";
	private static final String FIELD_BUTTON_UPDATE = "update";
	
	private static final String COL_LABEL = "label";
	private static final String COL_CONTROL = "control";
	private static final String COL_BUTTON = "button"; 
	
	private static final String ROW_SAGEHOME = "sagehome";
	private static final String ROW_PLUGINS="plugins";
	private static final String ROW_BUTTONS="update";
	private static final String ROW_LOG="log";
	private static final String ROW_OPTIONS = "options";
	private static final String FIELD_OFFLINE = "offline";


	private static EventBus bus = new EventBus();
	
	private PhoenixInstaller pi;
	private JFrame frame;
	
	FormLayoutHelper form;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PhoenixInstallerUI2 window = new PhoenixInstallerUI2();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws Exception 
	 */
	public PhoenixInstallerUI2() throws Exception {
		createUI();

		// create the phoenix installer instance
		pi = new PhoenixInstaller(this);

		// set the default dir
		FormLayoutHelper.postUpdate(bus, FIELD_SAGETV_HOME, new File(".").getAbsolutePath());

		
		// connect to buttons
		form.connectClick(FIELD_BUTTON_CHOOSE_SAGETV, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setDialogTitle("Select SageTV Home");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setAcceptAllFileFilterUsed(false);
				if (chooser.showOpenDialog(form.container()) == JFileChooser.APPROVE_OPTION) {
					form.setValue(FIELD_SAGETV_HOME, chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		
		form.connectClick(FIELD_BUTTON_UPDATE, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							pi.sageHome = new File((String) form.getValue(FIELD_SAGETV_HOME));
							System.out.println("SELECTED: " + form.getValue(FIELD_OFFLINE));
							pi.getPluginManager().setUseResolvers((Boolean)form.getValue(FIELD_OFFLINE));
							pi.install((String) form.getValue(FIELD_PLUGINS));
						}
					});
					t.start();
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(form.container(),	"Failed to load plugin manager");
				}
			}
		});
		
		form.connectUpdate(FIELD_SAGETV_HOME, this);
		
		// default handler
		bus.register(this);
		
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void createUI() {
		frame = new JFrame("Phoenix Plugin Updater 1.3");
		frame.setBounds(100, 100, 640, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		form = new FormLayoutHelper(bus, frame.getContentPane());
		form.addCol(COL_LABEL, ColumnSpec.decode("default"));
		form.addCol(COL_CONTROL, ColumnSpec.decode("default:grow"));
		form.addCol(COL_BUTTON, FormFactory.DEFAULT_COLSPEC);
		
		form.addRow(ROW_SAGEHOME, FormFactory.DEFAULT_ROWSPEC);
		form.addRow(ROW_PLUGINS, FormFactory.DEFAULT_ROWSPEC);
		form.addRow(ROW_LOG, RowSpec.decode("default:grow"));

		form.addRow(ROW_OPTIONS, FormFactory.DEFAULT_ROWSPEC);

		form.addRow(ROW_BUTTONS, FormFactory.DEFAULT_ROWSPEC);
		
		form.add(COL_LABEL, ROW_SAGEHOME, "right", "default", new JLabel("SageTV Home"));
		form.add(FIELD_SAGETV_HOME, COL_CONTROL, ROW_SAGEHOME ,"fill", "default", new JTextField());
		form.add(FIELD_BUTTON_CHOOSE_SAGETV, COL_BUTTON, ROW_SAGEHOME, "default", "default", new JButton("..."));

		form.add(COL_LABEL, ROW_PLUGINS, "right", "default", new JLabel("Plugin ID"));
		form.add(FIELD_PLUGINS, COL_CONTROL, ROW_PLUGINS, "fill", "default", new JComboBox());
	
		form.add(FIELD_OFFLINE, COL_LABEL, ROW_OPTIONS, form.maxColSpan(), 1, "fill", "default", new JCheckBox("SageTV.COM Offline"));
		
		form.add(FIELD_BUTTON_UPDATE, COL_BUTTON, ROW_BUTTONS, "default", "default", new JButton("Update"));

		JScrollPane scrollPane = new JScrollPane();
		JTextArea textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		form.add(COL_LABEL, ROW_LOG, form.maxColSpan(), 1, "fill", "fill", scrollPane);
		form.manage(FIELD_LOG, textArea);
	}
	
	public void msg(final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JTextArea ta = form.getComponent(FIELD_LOG);
				ta.append(msg + "\n");
			}
		});
	}

	@Override
	public void valueUpdated(ValueUpdatedEvent e) {
		File jars = new File(new File((String)form.getValue(FIELD_SAGETV_HOME)), "JARs");
		
		((JComboBox)form.getComponent(FIELD_PLUGINS)).setEnabled(jars.exists());
		((JButton)form.getComponent(FIELD_BUTTON_UPDATE)).setEnabled(jars.exists());

		File plugins = new File(new File((String)form.getValue(FIELD_SAGETV_HOME)), PluginManager.PLUGINS_FILE);

		if (plugins.exists()) {
			try {
				pi.getPluginManager().setPluginModel(new SageTVPluginModelImpl(plugins.toURL()));
			} catch (Throwable e1) {
				msg("SageTVPlugins file is missing or corrupt.  Using embedded plugins file.");
				try {
					pi.getPluginManager().setPluginModel(new SageTVPluginModelImpl(PhoenixInstaller.class.getResource(PluginManager.PLUGINS_FILE)));
				} catch (Exception e2) {
					e2.getStackTrace();
				}
			}
		} else {
			try {
				pi.getPluginManager().setPluginModel(new SageTVPluginModelImpl(PhoenixInstaller.class.getResource(PluginManager.PLUGINS_FILE)));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

			// set the plugins
			JComboBox cplugins = form.getComponent(FIELD_PLUGINS);
			List<String> list = new ArrayList<String>(pi.getPluginManager().getPlugins().keySet());
			Collections.sort(list);
			cplugins.setModel(new DefaultComboBoxModel(list.toArray()));
			cplugins.setSelectedItem("phoenix");
	}
}
