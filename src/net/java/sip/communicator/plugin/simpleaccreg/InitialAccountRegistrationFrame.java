/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.simpleaccreg;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>NoAccountFoundPage</tt> is the page shown in the account
 * registration wizard shown in the beginning of the program, when no registered
 * accounts are found.
 * 
 * @author Yana Stamcheva
 */
public class InitialAccountRegistrationFrame
    extends JFrame
    implements ServiceListener
{
    private final Logger logger
        = Logger.getLogger(InitialAccountRegistrationFrame.class);

    private JPanel mainAccountsPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel accountsPanel = new JPanel(new GridLayout(0, 2, 10, 10));

    private JButton signinButton = new JButton(Resources.getString("signin"));

    private Collection registrationForms = new Vector();

    /**
     * Creates an instance of <tt>NoAccountFoundPage</tt>.
     */
    public InitialAccountRegistrationFrame()
    {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        MainPanel mainPanel = new MainPanel(new BorderLayout(5, 5));
        JPanel messageAreaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JTextArea messageArea =
            new JTextArea(Resources.getString("initialAccountRegistration"));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton(Resources.getString("cancel"));

        this.setTitle(Resources.getString("signin"));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.getContentPane().add(mainPanel);

        mainPanel.add(messageAreaPanel, BorderLayout.NORTH);
        mainPanel.add(mainAccountsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        messageAreaPanel.add(messageArea);
        messageArea.setPreferredSize(new Dimension(350, 20));
        messageArea.setFont(messageArea.getFont().deriveFont(Font.BOLD));

        mainAccountsPanel.add(accountsPanel, BorderLayout.CENTER);

        mainAccountsPanel.setOpaque(false);
        accountsPanel.setOpaque(false);
        buttonPanel.setOpaque(false);
        messageArea.setOpaque(false);
        messageAreaPanel.setOpaque(false);

        SigninActionListener actionListener = new SigninActionListener();

        signinButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);

        buttonPanel.add(cancelButton);
        buttonPanel.add(signinButton);

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);

        this.getRootPane().setDefaultButton(signinButton);

        this.initAccountWizards();
        
        // Create the default group
        String groupName = Resources.getApplicationProperty("defaultGroupName");
        
        if(groupName != null && groupName.length() > 0)
        {
            MetaContactListService contactList =
                SimpleAccountRegistrationActivator.getContactList();
            Iterator iter = contactList.getRoot().getSubgroups();
            while (iter.hasNext())
            {
                MetaContactGroup gr = (MetaContactGroup) iter.next();
                if (groupName.equals(gr.getGroupName()))
                    return;
            }

            contactList
                .createMetaContactGroup(contactList.getRoot(), groupName);

            SimpleAccountRegistrationActivator.getConfigurationService().
                setProperty(
                "net.java.sip.communicator.impl.gui.addcontact.lastContactParent",
                groupName
            );
        }
    }

    private void initAccountWizards()
    {
        String simpleWizards = Resources.getLoginProperty("simpleWizards");

        StringTokenizer tokenizer = new StringTokenizer(simpleWizards, "|");

        ServiceReference[] serviceRefs = null;
        while (tokenizer.hasMoreTokens())
        {
            String protocolToken = tokenizer.nextToken();

            String osgiFilter = "("
                + ProtocolProviderFactory.PROTOCOL
                + "="+protocolToken+")";

            try
            {
                serviceRefs = SimpleAccountRegistrationActivator.bundleContext
                    .getServiceReferences(
                        AccountRegistrationWizard.class.getName(), osgiFilter);

                if (serviceRefs != null && serviceRefs.length > 0)
                {
                    AccountRegistrationWizard wizard
                        = (AccountRegistrationWizard)

                        SimpleAccountRegistrationActivator
                        .bundleContext.getService(serviceRefs[0]);

                    this.addAccountRegistrationForm(wizard);
                }
            }
            catch (InvalidSyntaxException ex)
            {
                logger.error("GuiActivator : ", ex);
            }
        }
    }

    /**
     * 
     */
    private class AccountRegistrationPanel extends JPanel
    {
        private JLabel protocolLabel = new JLabel();

        private JLabel usernameLabel = new JLabel("Login");

        private JLabel passwordLabel = new JLabel("Password");

        private JTextField usernameField = new JTextField();

        private JLabel usernameExampleLabel = new JLabel();

        private JPasswordField passwordField = new JPasswordField();

        private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 5, 0));

        private JPanel fieldsPanel = new JPanel(new GridLayout(0, 1, 5, 0));

        private JPanel emptyPanel = new JPanel();

        private JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        private JPanel iconDescriptionPanel = new JPanel(new BorderLayout());

        private JPanel inputRegisterPanel = new JPanel(new BorderLayout());

        private JTextArea descriptionArea = new JTextArea();

        private JLabel signupLabel
            = new JLabel("<html><a href=''>"
                + Resources.getString("signup")
                + "</a></html>", JLabel.RIGHT);

        private JLabel specialSignupLabel
            = new JLabel("<html><a href=''>"
                + Resources.getString("specialSignup")
                + "</a></html>", JLabel.RIGHT);

        private AccountRegistrationWizard wizard;

        public AccountRegistrationPanel(
            AccountRegistrationWizard accountWizard,
            boolean isPreferredWizard)
        {
            super(new BorderLayout(5, 5));

            this.wizard = accountWizard;

            this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            this.setPreferredSize(new Dimension(230, 150));

            this.setOpaque(false);

            this.inputPanel.setOpaque(false);
            this.labelsPanel.setOpaque(false);
            this.fieldsPanel.setOpaque(false);
            this.emptyPanel.setOpaque(false);
            this.inputRegisterPanel.setOpaque(false);
            this.iconDescriptionPanel.setOpaque(false);

            this.add(inputRegisterPanel, BorderLayout.CENTER);

            this.inputRegisterPanel.add(inputPanel, BorderLayout.NORTH);

            if (wizard.isWebSignupSupported())
            {
                if (isPreferredWizard)
                {
                    this.inputRegisterPanel.add(
                            specialSignupLabel, BorderLayout.SOUTH);
                }
                else
                {
                    this.inputRegisterPanel.add(
                            signupLabel, BorderLayout.SOUTH);
                }
            }

            this.inputPanel.add(labelsPanel, BorderLayout.WEST);

            this.inputPanel.add(fieldsPanel, BorderLayout.CENTER);

            this.iconDescriptionPanel.add(
                protocolLabel, BorderLayout.NORTH);

            this.signupLabel.setFont(signupLabel.getFont().deriveFont(10f));
            this.signupLabel.addMouseListener(new MouseAdapter()
                {
                    public void mousePressed(MouseEvent arg0)
                    {
                        try
                        {
                            wizard.webSignup();
                        }
                        catch (UnsupportedOperationException e)
                        {
                            // This should not happen, because we check if the
                            // operation is supported, before adding the sign up.
                            logger.error("The web sign up is not supported.", e);
                        }
                    }
                });

            this.specialSignupLabel.setFont(signupLabel.getFont().deriveFont(10f));
            this.specialSignupLabel.addMouseListener(new MouseAdapter()
                {
                    public void mousePressed(MouseEvent arg0)
                    {
                        try
                        {
                            wizard.webSignup();
                        }
                        catch (UnsupportedOperationException e)
                        {
                            // This should not happen, because we check if the
                            // operation is supported, before adding the sign up.
                            logger.error("The web sign up is not supported.", e);
                        }
                    }
                });

            this.protocolLabel.setFont(
                protocolLabel.getFont().deriveFont(Font.BOLD, 14f));
            this.usernameExampleLabel.setForeground(Color.DARK_GRAY);
            this.usernameExampleLabel.setFont(
                usernameExampleLabel.getFont().deriveFont(8f));
            this.emptyPanel.setMaximumSize(new Dimension(40, 25));

            this.labelsPanel.add(usernameLabel);
            this.labelsPanel.add(emptyPanel);
            this.labelsPanel.add(passwordLabel);

            this.fieldsPanel.add(usernameField);
            this.fieldsPanel.add(usernameExampleLabel);
            this.fieldsPanel.add(passwordField);

            this.usernameExampleLabel.setText(wizard.getUserNameExample());

            this.protocolLabel.setText(wizard.getProtocolName());

            Image image = null;
            try
            {
                image = ImageIO.read(
                    new ByteArrayInputStream(wizard.getPageImage()));
            }
            catch (IOException e)
            {
                logger.error("Unable to load image.", e);
            }

            if (image != null)
            {
                image = image.getScaledInstance(28, 28, Image.SCALE_SMOOTH);

                protocolLabel.setIcon(new ImageIcon(image));
            }

            if (isPreferredWizard)
            {
                descriptionArea.setBorder(BorderFactory
                    .createEmptyBorder(10, 0, 0, 0));

                descriptionArea.setFont(
                    descriptionArea.getFont().deriveFont(10f));
                descriptionArea.setPreferredSize(new Dimension(220, 50));
                descriptionArea.setLineWrap(true);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setText(wizard.getProtocolDescription());
                descriptionArea.setOpaque(false);

                this.iconDescriptionPanel.add(
                    descriptionArea, BorderLayout.CENTER);

                this.add(iconDescriptionPanel, BorderLayout.WEST);
            }
            else
            {
                this.add(iconDescriptionPanel, BorderLayout.NORTH);
            }
        }

        public void paintComponent(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // do the superclass behavior first
            super.paintComponent(g2d);

            g2d.setColor(new Color(
                Resources.getColor("desktopBackgroundColor")));

            // paint the background with the chosen color
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        }

        public boolean isFilled()
        {
            if(usernameField.getText() != null
                && usernameField.getText().length() > 0)
                return true;

            return false;
        }

        public void signin()
        {
            ProtocolProviderService protocolProvider
                = wizard.signin(  usernameField.getText(),
                            new String(passwordField.getPassword()));

            saveAccountWizard(protocolProvider, wizard);
        }
    }

    /**
     * Handles registration of a new account wizard.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = SimpleAccountRegistrationActivator.bundleContext.
            getService(event.getServiceReference());

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof AccountRegistrationWizard))
            return;

        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
                this.addAccountRegistrationForm(wizard);
        }
    }

    /**
     * Adds a simple account registration form corresponding to the given
     * <tt>AccountRegistrationWizard</tt>.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt>, which gives us
     * information to fill our simple form.
     */
    private void addAccountRegistrationForm(AccountRegistrationWizard wizard)
    {
     // We don't need to add wizards that are not interested in a
        // simple sign in form.
        if (!wizard.isSimpleFormEnabled())
            return;

        String preferredWizardName
            = Resources.getLoginProperty("preferredAccountWizard");

        AccountRegistrationPanel accountPanel;

        if (preferredWizardName != null
            && preferredWizardName.equals(wizard.getClass().getName()))
        {
            accountPanel = new AccountRegistrationPanel(wizard, true);

            mainAccountsPanel.add(
                accountPanel,
                BorderLayout.NORTH);
        }
        else
        {
            accountPanel = new AccountRegistrationPanel(wizard, false);

            this.accountsPanel.add(accountPanel);
        }

        this.registrationForms.add(accountPanel);

        this.pack();
    }

    /**
     * Handles the event triggered by the "Signin" button.
     */
    private class SigninActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            JButton button = (JButton) evt.getSource();

            if (button.equals(signinButton))
            {
                Iterator regIterator = registrationForms.iterator();

                if (regIterator.hasNext())
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                while(regIterator.hasNext())
                {
                    AccountRegistrationPanel regForm
                        = (AccountRegistrationPanel) regIterator.next();

                    if (regForm.isFilled())
                    {
                        regForm.signin();
                    }
                }
            }

            InitialAccountRegistrationFrame initialAccountRegistrationFrame =
                InitialAccountRegistrationFrame.this;
            initialAccountRegistrationFrame.setVisible(false);
            initialAccountRegistrationFrame.dispose();
        }
    }

    private class MainPanel extends JPanel
    {
        public MainPanel(LayoutManager layoutManager)
        {
            super(layoutManager);
        }

        public void paintComponent(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // do the superclass behavior first
            super.paintComponent(g2d);

            g2d.setColor(new Color(
                Resources.getColor("accountRegistrationBackground")));

            // paint the background with the chosen color
            g2d.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 15, 15);
        }
    }

    /**
     * Saves the (protocol provider, wizard) pair in through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param protocolProvider the protocol provider to save
     * @param wizard the wizard to save
     */
    private void saveAccountWizard(ProtocolProviderService protocolProvider,
        AccountRegistrationWizard wizard)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        ConfigurationService configService
            = SimpleAccountRegistrationActivator.getConfigurationService();

        List accounts = configService.getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;
        Iterator accountsIter = accounts.iterator();

        while (accountsIter.hasNext())
        {
            String accountRootPropName = (String) accountsIter.next();

            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID()
                .getAccountUniqueID()))
            {

                configService.setProperty(accountRootPropName + ".wizard",
                    wizard.getClass().getName().replace('.', '_'));

                savedAccount = true;
            }
        }

        if (!savedAccount)
        {
            String accNodeName =
                "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage =
                "net.java.sip.communicator.impl.gui.accounts." + accNodeName;

            configService.setProperty(accountPackage, protocolProvider
                .getAccountID().getAccountUniqueID());

            configService.setProperty(accountPackage + ".wizard", wizard);
        }
    }

}
