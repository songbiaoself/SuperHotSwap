package com.coderevolt.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RemoteConfigDialog extends JDialog  {

    private JTable table;
    private DefaultTableModel model;
    private RemoteConfigState state;


    public RemoteConfigDialog(RemoteConfigState state) {
        this.state = state;
        initUI(state.entries);
        initData(state.entries);
    }

    private void initData(List<RemoteConfigState.Entry> rows) {
        for (RemoteConfigState.Entry row : rows) {
            model.addRow(new Object[]{row.getProcessName(), row.getIp(), row.getPort(), row.getLastHeartBeat()});
        }
    }

    private void initUI(List<RemoteConfigState.Entry> rows) {
        setVisible(false);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = 600;
        int height = 500;
        setBounds((screenSize.width - width) / 2, (screenSize.height - height) / 2, width, height);
        setTitle("Remote Config");

        model = new DefaultTableModel(new Object[]{"ProcessName", "IP", "Port", "LastHeartBeat"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 3;
            }
        };
        table = new JBTable(model);

        JScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JButton addButton = new JButton("Add");
        JButton deleteButton = new JButton("Delete");

        addButton.addActionListener(e -> addNewItem());
        deleteButton.addActionListener(e -> deleteSelectedItem());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.NORTH);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        JButton okButton = new JButton("Apply");
        okButton.addActionListener(e -> applyUpdate());
        footerPanel.add(okButton);
        add(footerPanel, BorderLayout.SOUTH);

    }

    private void applyUpdate() {
        List<RemoteConfigState.Entry> news = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String processName = (String) model.getValueAt(i, 0);
            String ip = (String) model.getValueAt(i, 1);
            String port = (String) model.getValueAt(i, 2);
            String lastHeartBeat = (String) model.getValueAt(i, 3);
            RemoteConfigState.Entry t = new RemoteConfigState.Entry(processName, ip, port, lastHeartBeat);
            news.add(t);
        }
        state.entries.clear();
        state.entries.addAll(news);
    }


    private void deleteSelectedItem() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            model.removeRow(selectedRow);
        }
    }

    private void addNewItem() {
        model.addRow(new Object[]{});
    }


}