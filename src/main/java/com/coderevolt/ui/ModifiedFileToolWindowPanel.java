package com.coderevolt.ui;

import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.ModifiedFileTracker;
import com.coderevolt.context.VirtualMachineContext;
import com.coderevolt.handler.HandlerStrategyFactory;
import com.coderevolt.listener.ModifiedFileListener;
import com.coderevolt.util.IdeaNotifyUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class ModifiedFileToolWindowPanel implements Disposable {

    private final Project project;
    private final ModifiedFileTracker tracker;
    private final HandlerStrategyFactory handlerStrategyFactory = new HandlerStrategyFactory();

    private final DefaultListModel<ModifiedFileItem> listModel = new DefaultListModel<>();
    private final JBList<ModifiedFileItem> fileList = new JBList<>(listModel);
    private final JComboBox<String> processComboBox = new JComboBox<>();
    private final JBTextField directoryFilterField = new JBTextField();
    private final JBLabel filterStatusLabel = new JBLabel();
    private final JButton refreshProcessButton = new JButton("刷新进程");
    private final JButton selectAllButton = new JButton("全选");
    private final JButton clearButton = new JButton("清空记录");
    private final JButton hotSwapButton = new JButton("热更新选中");
    private final JPanel panel;

    public ModifiedFileToolWindowPanel(Project project) {
        this.project = project;
        this.tracker = project.getService(ModifiedFileTracker.class);
        this.directoryFilterField.setText(tracker.getDirectoryExcludeRegex());
        this.filterStatusLabel.setForeground(JBColor.RED);
        this.panel = buildPanel();
        bindListeners();
        refreshProcessList();
        refreshFileList();
    }

    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void dispose() {
        // no-op
    }

    private JPanel buildPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(JBUI.Borders.empty(8));

        JPanel processPanel = new JPanel(new BorderLayout(8, 0));
        processPanel.add(new JBLabel("目标进程"), BorderLayout.WEST);
        processPanel.add(processComboBox, BorderLayout.CENTER);
        processPanel.add(refreshProcessButton, BorderLayout.EAST);
        JPanel filterPanel = new JPanel(new BorderLayout(8, 0));
        filterPanel.setBorder(JBUI.Borders.emptyTop(4));
        filterPanel.add(new JBLabel("目录过滤正则"), BorderLayout.WEST);
        filterPanel.add(directoryFilterField, BorderLayout.CENTER);
        filterPanel.add(filterStatusLabel, BorderLayout.EAST);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(processPanel);
        topPanel.add(filterPanel);
        root.add(topPanel, BorderLayout.NORTH);

        fileList.setCellRenderer(new ModifiedFileItemRenderer());
        fileList.getEmptyText().setText("暂无修改记录");
        root.add(new JBScrollPane(fileList), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.add(selectAllButton);
        actionPanel.add(clearButton);
        actionPanel.add(hotSwapButton);
        root.add(actionPanel, BorderLayout.SOUTH);

        return root;
    }

    private void bindListeners() {
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = fileList.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                Rectangle bounds = fileList.getCellBounds(index, index);
                if (bounds == null || !bounds.contains(e.getPoint())) {
                    return;
                }
                ModifiedFileItem item = listModel.getElementAt(index);
                item.setSelected(!item.isSelected());
                fileList.repaint(bounds);
            }
        });

        selectAllButton.addActionListener(e -> setAllSelected(true));
        clearButton.addActionListener(e -> tracker.clear());
        refreshProcessButton.addActionListener(e -> refreshProcessList());
        hotSwapButton.addActionListener(e -> doHotSwap());

        project.getMessageBus().connect(this).subscribe(ModifiedFileListener.TOPIC, () ->
                ApplicationManager.getApplication().invokeLater(this::refreshFileList)
        );

        directoryFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyDirectoryFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyDirectoryFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyDirectoryFilter();
            }
        });
    }

    private void refreshFileList() {
        Set<String> selectedPaths = new HashSet<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            ModifiedFileItem item = listModel.getElementAt(i);
            if (item.isSelected()) {
                selectedPaths.add(item.getPath());
            }
        }

        List<VirtualFile> files = tracker.listModifiedFiles();
        files.sort(Comparator.comparing(VirtualFile::getPath));

        listModel.clear();
        List<VirtualFile> invalidFiles = new ArrayList<>();
        for (VirtualFile file : files) {
            if (file == null || !file.isValid()) {
                if (file != null) {
                    invalidFiles.add(file);
                }
                continue;
            }
            String path = file.getPath();
            ModifiedFileItem item = new ModifiedFileItem(file, toDisplayPath(path));
            item.setSelected(selectedPaths.contains(path));
            listModel.addElement(item);
        }
        if (!invalidFiles.isEmpty()) {
            tracker.removeFiles(invalidFiles);
        }
    }

    private void refreshProcessList() {
        String selected = (String) processComboBox.getSelectedItem();
        processComboBox.removeAllItems();

        String locationHash = project.getLocationHash();
        List<String> names = new ArrayList<>();
        for (MachineBeanInfo process : VirtualMachineContext.values()) {
            if (locationHash.equals(process.getProjectLocationHash())) {
                names.add(process.getProcessName());
            }
        }
        names.sort(String::compareToIgnoreCase);
        for (String name : names) {
            processComboBox.addItem(name);
        }
        if (selected != null && names.contains(selected)) {
            processComboBox.setSelectedItem(selected);
        }
    }

    private void setAllSelected(boolean selected) {
        for (int i = 0; i < listModel.getSize(); i++) {
            listModel.getElementAt(i).setSelected(selected);
        }
        fileList.repaint();
    }

    private void doHotSwap() {
        String processName = (String) processComboBox.getSelectedItem();
        if (processName == null || processName.trim().isEmpty()) {
            IdeaNotifyUtil.notify("请选择要热更新的进程", NotificationType.WARNING, null, project);
            return;
        }
        List<VirtualFile> selectedFiles = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            ModifiedFileItem item = listModel.getElementAt(i);
            if (item.isSelected()) {
                selectedFiles.add(item.getFile());
            }
        }
        if (selectedFiles.isEmpty()) {
            IdeaNotifyUtil.notify("请先选择要热更新的文件", NotificationType.WARNING, null, project);
            return;
        }

        handlerStrategyFactory.doAction(project, processName, selectedFiles, response -> {
            if (response.isOk()) {
                ApplicationManager.getApplication().invokeLater(() -> tracker.removeFiles(selectedFiles));
            }
        });
    }

    private void applyDirectoryFilter() {
        String regex = directoryFilterField.getText();
        boolean ok = tracker.updateDirectoryExcludeRegex(regex);
        if (ok) {
            filterStatusLabel.setText("");
            refreshFileList();
        } else {
            filterStatusLabel.setText("正则无效");
        }
    }

    private String toDisplayPath(String fullPath) {
        String basePath = project.getBasePath();
        if (basePath != null && fullPath.startsWith(basePath)) {
            int start = basePath.length();
            if (fullPath.length() > start && (fullPath.charAt(start) == '/' || fullPath.charAt(start) == '\\')) {
                return fullPath.substring(start + 1);
            }
        }
        return fullPath;
    }

    private static final class ModifiedFileItem {
        private final VirtualFile file;
        private final String displayPath;
        private boolean selected;

        private ModifiedFileItem(VirtualFile file, String displayPath) {
            this.file = file;
            this.displayPath = displayPath;
        }

        public VirtualFile getFile() {
            return file;
        }

        public String getPath() {
            return file.getPath();
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getDisplayPath() {
            return displayPath;
        }
    }

    private static final class ModifiedFileItemRenderer extends JCheckBox implements ListCellRenderer<ModifiedFileItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ModifiedFileItem> list,
                                                      ModifiedFileItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(value == null ? "" : value.getDisplayPath());
            setSelected(value != null && value.isSelected());
            setOpaque(true);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }
}
