package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.ColourDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.JUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Print dialog for deck printing. Allows virtual paper sizes to be split over
 * multiple physical pages.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class PrintSetupDialog extends javax.swing.JDialog implements ActionListener, AgnosticDialog {

    private final DeckEditor editor;
    private final NumberFormat formatter;

    public PrintSetupDialog(DeckEditor editor) {
        super((JFrame) SwingUtilities.getWindowAncestor(editor), true);
        setIconImages(StrangeEons.getWindow().getIconImages());
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
        formatter = NumberFormat.getNumberInstance();

        this.editor = editor;

        Deck deck = editor.getDeck();
        PaperProperties sel = deck.getPrinterPaperProperties();
        initPaperList(sel);
        splitPageCheck.setSelected(deck.isPaperSplitting());
        borderSize.setText(formatter.format(deck.getSplitBorder()));
        borderColor.setBackground(deck.getSplitBorderColor());
        updateSplitPanelState();

        pdfBtn.setVisible(PDFPrintSupport.isAvailable());

        pack();
        JUtilities.snapToPointer(this);
    }

    private void initPaperList(PaperProperties sel) {
        Set<PaperProperties> papers = editor.getDeck().getPrinterPaperSizes();

        if (sel == null) {
            sel = PaperSets.getDefaultPaper(papers);
        } else {
            sel = PaperSets.findBestPaper(sel, papers);
        }

        LinkedHashSet<PaperProperties> filtered = new LinkedHashSet<>();
        filtered.add(sel);
        PaperProperties layout = editor.getDeck().getPrinterPaperProperties();
        for (PaperProperties pp : papers) {
            if (pp.getPageWidth() < layout.getPageWidth() || pp.getPageHeight() < layout.getPageHeight()) {
                filtered.add(pp);
            }
        }
        papers = filtered;

        paperCombo.setModel(PaperSets.setToComboBoxModel(papers));
        paperCombo.setSelectedItem(sel);
    }

    private void updateSplitPanelState() {
        JUtilities.enableTree(splitPanel, splitPageCheck.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        paperCombo = new javax.swing.JComboBox();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        borderSize = new javax.swing.JTextField();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        borderColor = new ColourDialog.ColourButton();
        cancelBtn = new javax.swing.JButton();
        pdfBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        splitPageCheck = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("de-title-split-paper")); // NOI18N

        splitPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("de-l-printer-paper-settings"))); // NOI18N

        jLabel1.setText(string("de-l-printer-paper")); // NOI18N

        paperCombo.setMaximumRowCount(12);
        paperCombo.setRenderer( new JIconList.IconRenderer() );

        jLabel2.setText(string("de-l-frame")); // NOI18N

        borderSize.setColumns(6);
        borderSize.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        borderSize.setText("0");

        jLabel3.setText(string("de-l-points")); // NOI18N

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getSize()-2f));
        jLabel4.setText(string("de-l-frame-desc")); // NOI18N

        borderColor.setPreferredSize(new java.awt.Dimension(24, 24));
        borderColor.addActionListener(this);

        javax.swing.GroupLayout splitPanelLayout = new javax.swing.GroupLayout(splitPanel);
        splitPanel.setLayout(splitPanelLayout);
        splitPanelLayout.setHorizontalGroup(
            splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(splitPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(splitPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(paperCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(splitPanelLayout.createSequentialGroup()
                        .addGroup(splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(splitPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(borderColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(borderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel4)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        splitPanelLayout.setVerticalGroup(
            splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(splitPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(splitPanelLayout.createSequentialGroup()
                        .addGroup(splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(paperCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(borderColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(splitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(borderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)
                        .addComponent(jLabel4)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelBtn.setText(string("cancel")); // NOI18N

        pdfBtn.setText(string("psd-pdf")); // NOI18N
        pdfBtn.addActionListener(this);

        okBtn.setText(string("psd-ok")); // NOI18N

        splitPageCheck.setText(string("de-b-split")); // NOI18N
        splitPageCheck.addActionListener(this);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(splitPageCheck)
                        .addGap(0, 80, Short.MAX_VALUE))
                    .addComponent(splitPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(pdfBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitPageCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(splitPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(pdfBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getSource() == borderColor) {
            PrintSetupDialog.this.borderColorActionPerformed(evt);
        }
        else if (evt.getSource() == pdfBtn) {
            PrintSetupDialog.this.pdfBtnActionPerformed(evt);
        }
        else if (evt.getSource() == splitPageCheck) {
            PrintSetupDialog.this.splitPageCheckActionPerformed(evt);
        }
    }// </editor-fold>//GEN-END:initComponents

private void splitPageCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_splitPageCheckActionPerformed
    updateSplitPanelState();
}//GEN-LAST:event_splitPageCheckActionPerformed

    private void borderColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borderColorActionPerformed
        ColourDialog d = new ColourDialog(StrangeEons.getWindow());
        d.setSelectedColor(borderColor.getBackground());
        d.setLocationRelativeTo(borderColor);
        d.setVisible(true);
        borderColor.setBackground(d.getSelectedColor());
    }//GEN-LAST:event_borderColorActionPerformed

    private void pdfBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfBtnActionPerformed
        String baseName = editor.getDeck().getFullName();
        if (baseName == null || baseName.isEmpty()) {
            baseName = string("prj-doc-title");
        }
        baseName = ResourceKit.makeStringFileSafe(baseName);
        File sel = ResourceKit.showGenericExportFileDialog(this, baseName, string("psd-pdf-desc"), "pdf");
        if (sel != null) {
            applyPrintSettings();
            PDFPrintSupport.printToPDF(editor, sel);
            dispose();
        }
    }//GEN-LAST:event_pdfBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton borderColor;
    private javax.swing.JTextField borderSize;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton okBtn;
    private javax.swing.JComboBox paperCombo;
    private javax.swing.JButton pdfBtn;
    private javax.swing.JCheckBox splitPageCheck;
    private javax.swing.JPanel splitPanel;
    // End of variables declaration//GEN-END:variables

    private void applyPrintSettings() {
        Deck deck = editor.getDeck();

        deck.setPaperSplitting(splitPageCheck.isSelected());
        deck.setPrinterPaperProperties((PaperProperties) paperCombo.getSelectedItem());

        float border = deck.getSplitBorder();
        try {
            border = formatter.parse(borderSize.getText()).floatValue();
        } catch (ParseException e) {
        }
        if (border >= 0f) {
            deck.setSplitBorder(border);
        }

        deck.setSplitBorderColor(borderColor.getBackground());
    }

    @Override
    public void handleOKAction(ActionEvent e) {
        doTruePrint = true;
        applyPrintSettings();
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        applyPrintSettings();
        dispose();
    }

    public boolean showDialog() {
        doTruePrint = false;
        setVisible(true);
        return doTruePrint;
    }

    private boolean doTruePrint;
}
