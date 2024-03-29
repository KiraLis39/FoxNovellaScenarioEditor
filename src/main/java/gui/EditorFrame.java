package gui;

import components.tools.VerticalFlowLayout;
import config.Configuration;
import fox.Out;
import iom.JIOM;
import utils.FoxFontBuilder;
import utils.InputAction;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EditorFrame extends JFrame implements WindowListener, ActionListener {
    private final String scriptExtension = ".dingo";
    private Configuration config;
    private JFileChooser fch;
    private Path scenario;
    private LinkedList<String> lines;
    private LinkedList<String> answers;
//    private LinkedList<String> logics;
    private JTextArea textArea, answersArea;
    private JLabel lineCountLabel;
    private int lineIndex;
    private final Font f0 = FoxFontBuilder.setFoxFont(FoxFontBuilder.FONT.BAHNSCHRIFT, 20, false);
    private final Font f1 = FoxFontBuilder.setFoxFont(FoxFontBuilder.FONT.SEGOE_UI_SYMBOL, 20, true);
    private static JToolBar toolBar;
    private final Color baseColor = new Color(0.05f, 0.05f, 0.055f, 1.0f);
    private JComboBox<String> ownerBox, screenBox,
            backgBox, musicBox, soundBox, voiceBox,
            npcNameBox, npcTypeBox, npcMoodBox,
            metaChapterBox, metaNextDayBox;
    private JSpinner carmaSpinner;
    private JPanel answerBtnsPane, upOwnerPane, linesBtnsPane;
    private BufferedImage avatarImage;
    private JScrollPane lineScrollPane;
    private String nfLink, logicsLink;


    public EditorFrame() {
        setTitle("Novella scenario editor");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1440, 900));

        toolBar = new JToolBar("Можно тягать!") {
            {
                setLayout(new VerticalFlowLayout(0,0, 0));
                setOrientation(1);
                setBackground(baseColor);
                setBorder(new EmptyBorder(0, 0, 0, 0));

                JButton openBtn = new JButton("⮟") {
                    {
                        setFocusPainted(false);
                        setToolTipText("Открыть сценарий");
                        setForeground(Color.GREEN.darker());
                        setBackground(Color.BLACK);
                        setFont(f1);
                        setActionCommand("open");
                        addActionListener(EditorFrame.this);
                        setPreferredSize(new Dimension(64, 64));
                    }
                };

                JButton saveBtn = new JButton("\uD83D\uDCBE") {
                    {
                        setFocusPainted(false);
                        setToolTipText("Сохранить на диск");
                        setFont(f1);
                        setActionCommand("save");
                        setForeground(Color.ORANGE);
                        setBackground(Color.BLACK);
                        addActionListener(EditorFrame.this);
                        setPreferredSize(new Dimension(64, 64));
                    }
                };

                JButton addBtn = new JButton("➕") {
                    {
                        setFocusPainted(false);
                        setToolTipText("Добавить строку далее");
                        setFont(f1);
                        setActionCommand("addLine");
                        setForeground(Color.BLUE);
                        setBackground(Color.BLACK);
                        addActionListener(EditorFrame.this);
                        setPreferredSize(new Dimension(64, 64));
                    }
                };

                JButton removeBtn = new JButton("➖") {
                    {
                        setFocusPainted(false);
                        setToolTipText("Удалить эту строку");
                        setFont(f1);
                        setActionCommand("removeLine");
                        setForeground(Color.RED);
                        setBackground(Color.BLACK);
                        addActionListener(EditorFrame.this);
                        setPreferredSize(new Dimension(64, 64));
                    }
                };

                JButton resetBtn = new JButton("\uD83D\uDD04") {
                    {
                        setFocusPainted(false);
                        setToolTipText("Сбросить строку");
                        setFont(f1);
                        setActionCommand("resetLine");
                        setForeground(Color.YELLOW);
                        setBackground(Color.BLACK);
                        addActionListener(EditorFrame.this);
                        setPreferredSize(new Dimension(64, 64));
                    }
                };

//                JButton answersBtn = new JButton("\uD83D\uDD0E") {
//                    {
//                        setFocusPainted(false);
//                        setToolTipText("Редактировать ответы");
//                        setFont(f1);
//                        setActionCommand("answers");
//                        setForeground(Color.BLUE);
//                        setBackground(Color.BLACK);
//                        addActionListener(EditorFrame.this);
//                        setPreferredSize(new Dimension(64, 64));
//                    }
//                };

                add(openBtn);
                add(saveBtn);
                add(new JSeparator(0));
                add(new JSeparator(0));
                add(addBtn);
                add(removeBtn);
                add(new JSeparator(0));
                add(new JSeparator(0));
//                add(answersBtn);
//                add(new JSeparator(0));
//                add(new JSeparator(0));
                add(resetBtn);
            }
        };

        JPanel leftImagePane = new JPanel(new BorderLayout(0,0)) {
            {
                setBackground(baseColor);

                upOwnerPane = new JPanel() {
                    @Override
                    public void paintComponent(Graphics g) {
                        if (avatarImage != null) {
                            g.drawImage(avatarImage, 0,0, getWidth(), getHeight(), this);
                            g.setColor(Color.DARK_GRAY);
                            g.drawRoundRect(0,0,getWidth()-2,getHeight()-1,9,9);
                        }
                    }

                    {
                        setOpaque(false);
                        setPreferredSize(new Dimension(128, 128));
                    }
                };

                linesBtnsPane = new JPanel(new GridLayout(0,1,0,0));

                lineScrollPane = new JScrollPane(linesBtnsPane) {
                    {
                        setBorder(null);
                        getVerticalScrollBar().setUnitIncrement(48);
                    }
                };

                add(upOwnerPane, BorderLayout.NORTH);
                add(lineScrollPane, BorderLayout.CENTER);
            }
        };

        JPanel basePane = new JPanel(new BorderLayout(0, 0)) {
            {
                setOpaque(false);

                JPanel midContentPane = new JPanel(new BorderLayout(0,0)) {
                    {
                        setOpaque(false);

                        JPanel upFilterPane = new JPanel(new BorderLayout(0,0)) {
                            {
                                setOpaque(false);

                                JPanel contentPane = new JPanel(new GridLayout(2,4, 16,3)) {
                                    {
                                        setBackground(Color.PINK.darker().darker());
                                        setPreferredSize(new Dimension(0, 60));
                                        setBorder(new EmptyBorder(3, 6, 1,3));

                                        JPanel ownerPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Owner:");

                                                ownerBox = new JComboBox<>(new DefaultComboBoxModel<>()) {
                                                    {
                                                        setMaximumRowCount(15);
                                                    }
                                                };

                                                add(label, BorderLayout.WEST);
                                                add(ownerBox, BorderLayout.CENTER);
                                            }
                                        };
                                        JPanel screenPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Screen:");

                                                screenBox = new JComboBox<>(new DefaultComboBoxModel<>()) {
                                                    {
                                                        addActionListener(EditorFrame.this);
                                                    }
                                                };

                                                add(label, BorderLayout.WEST);
                                                add(screenBox, BorderLayout.CENTER);
                                            }
                                        };
                                        JPanel backgPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Backg: ");

                                                backgBox = new JComboBox<>(new DefaultComboBoxModel<>());

                                                add(label, BorderLayout.WEST);
                                                add(backgBox, BorderLayout.CENTER);
                                            }
                                        };
                                        JPanel musicPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Music:");

                                                musicBox = new JComboBox<>(new DefaultComboBoxModel<>());

                                                add(label, BorderLayout.WEST);
                                                add(musicBox, BorderLayout.CENTER);
                                            }
                                        };

                                        JPanel soundPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Sound:");

                                                soundBox = new JComboBox<>(new DefaultComboBoxModel<>());

                                                add(label, BorderLayout.WEST);
                                                add(soundBox, BorderLayout.CENTER);
                                            }
                                        };
                                        JPanel voicePane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Voice:   ");

                                                voiceBox = new JComboBox<>(new DefaultComboBoxModel<>());

                                                add(label, BorderLayout.WEST);
                                                add(voiceBox, BorderLayout.CENTER);
                                            }
                                        };
                                        JPanel carmaPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);

                                                JLabel label = new JLabel("Carma:");

                                                carmaSpinner = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));

                                                add(label, BorderLayout.WEST);
                                                add(carmaSpinner, BorderLayout.CENTER);
                                            }
                                        };

                                        add(ownerPane);
                                        add(screenPane);
                                        add(backgPane);
                                        add(musicPane);
                                        add(soundPane);
                                        add(voicePane);
                                        add(carmaPane);
                                        add(new JLabel(""));
                                    }
                                };

                                JPanel secondPane = new JPanel(new GridLayout(1,2,32,0)) {
                                    {
                                        setBackground(Color.PINK.darker());
                                        setPreferredSize(new Dimension(0, 60));
                                        setBorder(new EmptyBorder(3, 6, 3,3));

                                        JPanel npcerPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);
                                                setBorder(BorderFactory.createCompoundBorder(
                                                        new EmptyBorder(1, 6, 1,3),
                                                        BorderFactory.createTitledBorder(
                                                                BorderFactory.createLineBorder(Color.BLACK, 1, true),
                                                                "NPC name / type / mood:", 1, 0
                                                        )
                                                ));

                                                JPanel npcPane = new JPanel(new GridLayout(1,3,6,6)) {
                                                    {
                                                        setOpaque(false);

                                                        npcNameBox = new JComboBox<>(new DefaultComboBoxModel<>()) {
                                                            {
                                                                setActionCommand("npc");
                                                                addActionListener(EditorFrame.this);
                                                            }
                                                        };
                                                        npcTypeBox = new JComboBox<>(new DefaultComboBoxModel<>());
                                                        npcMoodBox = new JComboBox<>(new DefaultComboBoxModel<>());

                                                        add(npcNameBox);
                                                        add(npcTypeBox);
                                                        add(npcMoodBox);
                                                    }
                                                };

                                                add(npcPane, BorderLayout.CENTER);
                                            }
                                        };
                                        JPanel metaPane = new JPanel(new BorderLayout(6,0)) {
                                            {
                                                setOpaque(false);
                                                setBorder(BorderFactory.createCompoundBorder(
                                                        new EmptyBorder(1, 6, 1,3),
                                                        BorderFactory.createTitledBorder(
                                                                BorderFactory.createLineBorder(Color.BLACK, 1, true),
                                                                "Meta chapter / isNextDay:", 1, 0
                                                        )
                                                ));

                                                JPanel metaSubPane = new JPanel(new GridLayout(1, 2, 6, 6)) {
                                                    {
                                                        setOpaque(false);

                                                        metaChapterBox = new JComboBox<>(new DefaultComboBoxModel<>());
                                                        metaNextDayBox = new JComboBox<>(new DefaultComboBoxModel<>());

                                                        add(metaChapterBox);
                                                        add(metaNextDayBox);
                                                    }
                                                };

                                                add(metaSubPane, BorderLayout.CENTER);
                                            }
                                        };

                                        add(npcerPane);
                                        add(metaPane);
                                    }
                                };

                                add(contentPane, BorderLayout.NORTH);
                                add(secondPane, BorderLayout.CENTER);
                            }
                        };

                        JPanel centerTextPane = new JPanel(new GridLayout(2,1,0,3)) {
                            {
                                setBackground(baseColor);

                                textArea = new JTextArea() {
                                    {
                                        setBorder(new EmptyBorder(6, 6, 0,3));
                                        setWrapStyleWord(true);
                                        setLineWrap(true);
                                        setBackground(Color.DARK_GRAY);
                                        setForeground(Color.WHITE);
                                        setFont(f0);
                                    }
                                };

                                JPanel answersPane = new JPanel(new BorderLayout(0,0)) {
                                    {
                                        setOpaque(false);

                                        answersArea = new JTextArea() {
                                            {
                                                setBorder(new EmptyBorder(6, 6, 0,3));
                                                setWrapStyleWord(true);
                                                setLineWrap(true);
                                                setBackground(Color.DARK_GRAY);
                                                setForeground(Color.YELLOW);
                                                setFont(f0);
                                            }
                                        };

                                        answerBtnsPane = new JPanel(new GridLayout(1, 0, 3, 3)) {
                                            {
                                                setOpaque(false);
                                                setBorder(new EmptyBorder(0,0,0,0));
                                            }
                                        };

                                        add(answersArea, BorderLayout.CENTER);
                                        add(answerBtnsPane, BorderLayout.SOUTH);
                                    }
                                };

                                add(textArea);
                                add(answersPane);
                            }
                        };

                        add(upFilterPane, BorderLayout.NORTH);
                        add(centerTextPane, BorderLayout.CENTER);
                    }
                };

                JPanel downControlPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
                    {
                        setBackground(baseColor);
                        setBorder(new EmptyBorder(1, 3, 1, 0));

                        lineCountLabel = new JLabel("Строка N из X") {
                            {
                                setForeground(Color.GRAY);
                            }
                        };

                        JButton btnPrevLine = new JButton("previous") {
                            {
                                setActionCommand("prev");
                                setFocusPainted(false);
                                addActionListener(EditorFrame.this);
                                setBackground(Color.BLACK);
                                setForeground(Color.GRAY);
                            }
                        };

                        JButton btnNextLine = new JButton("next") {
                            {
                                setActionCommand("next");
                                setFocusPainted(false);
                                addActionListener(EditorFrame.this);
                                setBackground(Color.BLACK);
                                setForeground(Color.GRAY);
                            }
                        };

                        add(lineCountLabel);
                        add(btnPrevLine);
                        add(btnNextLine);
                    }
                };

                add(midContentPane, BorderLayout.CENTER);
                add(downControlPane, BorderLayout.SOUTH);
            }
        };

        inAc();

        add(toolBar, BorderLayout.EAST);
        add(basePane, BorderLayout.CENTER);
        add(leftImagePane, BorderLayout.WEST);

        addWindowListener(this);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        try {
            config = JIOM.fileToDto(Paths.get("./config" + scriptExtension) , Configuration.class);
            if (config.getLastOpenPath() != null) {
                scenario = config.getLastOpenPath();
                if (Files.exists(scenario)) {
                    reloadScript();
                } else {
                    throw new Exception("Script not exists: " + scenario);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(this,
                    "Ошибка загрузки конфигурации: " + e.getMessage(), "Ошибка:",
                    JOptionPane.DEFAULT_OPTION);
        }
    }

    private void buildComboBoxes() {
        List<Path> result;
        ArrayList<String> data = new ArrayList<>();

        try (Scanner scanner = new Scanner(Paths.get("./heroNamesList.txt"), StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                data.add(scanner.nextLine());
            }
            ownerBox.setModel(new DefaultComboBoxModel<>(data.toArray(new String[0])));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.clear();
        }

        try {
            data.add("-");
            data.add("clear");
            result = Files.walk(config.getScreensPath()).toList();
            for (Path path : result) {
                if (Files.isRegularFile(path)) {
                    data.add(path.getFileName().toString().substring(0, path.getFileName().toString().length()-4));
                }
            }
            screenBox.setModel(new DefaultComboBoxModel<>(data.toArray(new String[0])));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            data.clear();
        }

        try {
            data.add("-");
            data.add("stop");
            result = Files.walk(config.getBackgsPath()).toList();
            for (Path path : result) {
                if (Files.isRegularFile(path)) {
                    data.add(path.getFileName().toString().substring(0, path.getFileName().toString().length()-4));
                }
            }
            backgBox.setModel(new DefaultComboBoxModel<>(data.toArray(new String[0])));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.clear();
        }

        try {
            data.add("-");
            data.add("stop");
            result = Files.walk(config.getMusicPath()).toList();
            for (Path path : result) {
                if (Files.isRegularFile(path)) {
                    data.add(path.getFileName().toString().substring(0, path.getFileName().toString().length()-4));
                }
            }
            musicBox.setModel(new DefaultComboBoxModel<>(data.toArray(new String[0])));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.clear();
        }

        try {
            data.add("-");
            data.add("stop");
            result = Files.walk(config.getSoundPath()).toList();
            for (Path path : result) {
                if (Files.isRegularFile(path)) {
                    data.add(path.getFileName().toString().substring(0, path.getFileName().toString().length()-4));
                }
            }
            soundBox.setModel(new DefaultComboBoxModel<>(data.toArray(new String[0])));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.clear();
        }

        try {
            data.add("-");
            data.add("stop");
            result = Files.walk(config.getVoicePath()).toList();
            for (Path path : result) {
                if (Files.isRegularFile(path)) {
                    data.add(path.getFileName().toString().substring(0, path.getFileName().toString().length()-4));
                }
            }
            voiceBox.setModel(new DefaultComboBoxModel<>(data.toArray(new String[0])));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.clear();
        }

        String[] metaNpcNameBox = {"-", "Ann", "Dmitrii", "Kuro", "Lissa", "Mary", "Mishka", "Oksana", "Oleg", "Olga"};
        npcNameBox.setModel(new DefaultComboBoxModel<>(metaNpcNameBox));
        String[] metaNpcTypeBox = {"-", "everyday", "upWork", "sport", "sex", "home", "street", "dist", "mid"};
        npcTypeBox.setModel(new DefaultComboBoxModel<>(metaNpcTypeBox));
        String[] metaNpcMoodBox = {"-", "fear", "flirt", "fun", "sad", "shame", "simple", "sit", "udiv", "zlo", "cry", "die", "cum", "happy"};
        npcMoodBox.setModel(new DefaultComboBoxModel<>(metaNpcMoodBox));

        String[] metaChapter = {"-", "Часть 01", "Часть 02", "Часть 03", "Часть 04", "Часть 05"};
        metaChapterBox.setModel(new DefaultComboBoxModel<>(metaChapter));
        metaNextDayBox.setModel(new DefaultComboBoxModel<>(new String[]{"false", "true"}));
    }

    private void inAc() {
        InputAction.add("editor", this);
        InputAction.set("editor", "next", KeyEvent.VK_RIGHT, 0, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentLine();
                if (lineIndex + 1 < lines.size()) {
                    lineIndex++;
                    setPage();
                }
            }
        });
        InputAction.set("editor", "prev", KeyEvent.VK_LEFT, 0, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentLine();
                if (lineIndex > 0) {
                    lineIndex--;
                    setPage();
                }
            }
        });
    }

    private void reloadScript() throws IOException {
        buildComboBoxes();
        lines = new LinkedList<>(Files.readAllLines(scenario).stream().filter(s -> !s.isBlank() && !s.startsWith("var ") && !s.startsWith("nf ") && !s.startsWith("logic")).toList());
        answers = new LinkedList<>(Files.readAllLines(scenario).stream().filter(s -> !s.isBlank() && s.startsWith("var ")).toList());
//        logics = new LinkedList<>(Files.readAllLines(scenario).stream().filter(s -> !s.isBlank() && s.startsWith("logic")).toList());
        logicsLink = Files.readAllLines(scenario).stream().filter(s -> !s.isBlank() && s.startsWith("logic")).findFirst().orElse(null);

        nfLink = null;
        Optional<String> nfLinkTest = Files.readAllLines(scenario).stream().filter(s -> !s.isBlank() && s.startsWith("nf ")).findAny();
        if (nfLinkTest.isPresent()) {
            nfLink = nfLinkTest.get().replace("nf ", "").trim();
        }

        setTitle("Novella scenario editor [" + (scenario == null ? "NA" : scenario.getFileName()) + "]");
        lineIndex = 0;
        setPage();
        reloadAnswersBtns();
        reloadBtnsLinePanel();
        revalidate();
    }

    int index;
    private void reloadAnswersBtns() {
        answerBtnsPane.removeAll();
        for (String answer : answers) {
            answerBtnsPane.add(
                    new JButton(answer.split("R ")[1].trim()) {
                        {
                            setName(answer.split("R ")[2].trim());
                            setActionCommand("goNext");
                            addActionListener(EditorFrame.this);
                        }
                    });
        }
        if (nfLink != null) {
            answerBtnsPane.add(
                    new JButton(nfLink.replace("nf ", "").trim()) {
                        {
                            setName(nfLink.replace("nf ", "").trim());
                            setActionCommand("goNext");
                            addActionListener(EditorFrame.this);
                        }
                    });
        }

        if (logicsLink != null) {
            String[] logics = logicsLink.split(" ");
            for (String logic : logics) {
                if (logic.startsWith("logic")) {continue;}
                answerBtnsPane.add(
                        new JButton(logic.split("=")[0].trim()) {
                            {
                                setName(logic.split("=")[1].trim());
                                setActionCommand("goNext");
                                addActionListener(EditorFrame.this);
                            }
                        });
                index++;
            }
        }
    }

    private void reloadBtnsLinePanel() {
        linesBtnsPane.removeAll();
        for (int i = 0; i < lines.size(); i++) {
            int index = i + 1;
            linesBtnsPane.add(new JButton("" + index) {{
                setName("" + index);
//                setPreferredSize(new Dimension(16,16));
//                setSize(new Dimension(16,16));
                setActionCommand("toLineQuick");
                addActionListener(EditorFrame.this);
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
                setFocusPainted(false);
            }});
        }
        lineScrollPane.setPreferredSize(new Dimension(64, 32 * lines.size()));
        lineScrollPane.revalidate();
    }

    private void setPage() {
        if (lineIndex >= lines.size() && lineIndex > 0) {
            lineIndex--;
        }

        lineCountLabel.setText("Строка " + (lineIndex + 1) + " из " + lines.size());
        textArea.setText(parseLine(lines.get(lineIndex)));
        String answrs = "";
        for (String answer : answers) {
            answrs += answer + "\n";
        }
        answersArea.setText(answrs);
        lightSelectedButton();

        try {
            avatarImage = ImageIO.read(
                    new File(config.getHeroAvatarsPath() + "/" + convertRusNamesToAvatarName((String) ownerBox.getSelectedItem()) + ".png")
            );
        } catch (Exception e) {
            try {
                avatarImage = ImageIO.read(new File(config.getHeroAvatarsPath() + "/noImage.png"));
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        upOwnerPane.repaint();
    }

    private Object convertRusNamesToAvatarName(String rusName) {
        switch (rusName) {
            case "Аня" -> {return "Ann";}
            case "Дмитрий" -> {return "Dmitrii";}
            case "Куро" -> {return "Kuro";}
            case "Мари" -> {return "Mary";}
            case "Мишка" -> {return "Mishka";}
            case "Оксана" -> {return "Oksana";}
            case "Олег" -> {return "Oleg";}
            case "Ольга" -> {return "Olga";}
            case "Лисса" -> {return "Lissa";}
            default -> {return null;}
        }
    }

    private void lightSelectedButton() {
        for (Component component : linesBtnsPane.getComponents()) {
            if (component instanceof JButton) {
                if (Integer.parseInt(component.getName()) == lineIndex + 1) {
                    component.setBackground(Color.GREEN);
                    component.setForeground(Color.BLACK);
                } else {
                    component.setBackground(Color.DARK_GRAY);
                    component.setForeground(Color.WHITE);
                }
            }
        }
    }

    private void resetParams() {
        ownerBox.setSelectedIndex(0);
        screenBox.setSelectedIndex(0);
        backgBox.setSelectedIndex(0);
        musicBox.setSelectedIndex(0);
        soundBox.setSelectedIndex(0);
        voiceBox.setSelectedIndex(0);
        npcNameBox.setSelectedIndex(0);
        npcTypeBox.setSelectedIndex(0);
        npcMoodBox.setSelectedIndex(0);
        metaChapterBox.setSelectedIndex(0);
        metaNextDayBox.setSelectedIndex(0);
        carmaSpinner.setValue(0);
    }

    private String parseLine(String toParse) {
        resetParams();
        List<String> parsed = Arrays.stream(toParse.split(";")).map(String::trim).toList();
        String owner = parsed.get(0).split(":")[0];
        ownerBox.setSelectedItem(owner);

        for (String datum : parsed) {
            if (datum.startsWith("screen")) {
                screenBox.setSelectedItem(datum.split(":")[1]);
            }
            if (datum.startsWith("backg")) {
                backgBox.setSelectedItem(datum.split(":")[1]);
            }
            if (datum.startsWith("music")) {
                musicBox.setSelectedItem(datum.split(":")[1]);
            }
            if (datum.startsWith("sound")) {
                soundBox.setSelectedItem(datum.split(":")[1]);
            }
            if (datum.startsWith("voice")) {
                voiceBox.setSelectedItem(datum.split(":")[1]);
            }
            if (datum.startsWith("npc")) {
                String[] npcData = datum.split(":")[1].split(",");
                npcNameBox.setSelectedItem(npcData[0]);
                npcTypeBox.setSelectedItem(npcData[1]);
                npcMoodBox.setSelectedItem(npcData[2]);
            }
            if (datum.startsWith("meta")) {
                metaChapterBox.setSelectedItem(datum.split(":")[1].split(",")[0]);
                metaNextDayBox.setSelectedItem(datum.split(":")[1].split(",")[1]);
            }
            if (datum.startsWith("carma")) {
                carmaSpinner.setValue(Integer.parseInt(datum.split(":")[1]));
            }
            if (datum.startsWith("logic")) {
                logicsLink = datum;
//                logics = new LinkedList<>(Arrays.stream(datum.split(" ")).skip(1).toList());
            }
        }

        toParse = parsed.get(0).contains(":") ? parsed.get(0).split(":")[1].replace("\"", "") : parsed.get(0);
        return toParse;
    }

    private void saveCurrentLine() {
        if (lines != null && lines.size() > 0) {
            String text = textArea.getText();
            if (text.startsWith("var ") || text.startsWith("nf ") || text.startsWith("logic")) {
                lines.set(lineIndex, text);
            } else {
                String restoredLine = "";

                restoredLine += ownerBox.getSelectedItem() + ":\"" + text.replace("\n", "") + "\";";
                restoredLine += "screen:" + screenBox.getSelectedItem() + ";";
                restoredLine += "backg:" + backgBox.getSelectedItem() + ";";
                restoredLine += "music:" + musicBox.getSelectedItem() + ";";
                restoredLine += "sound:" + soundBox.getSelectedItem() + ";";
                restoredLine += "voice:" + voiceBox.getSelectedItem() + ";";
                restoredLine += "npc:" + npcNameBox.getSelectedItem() + "," + npcTypeBox.getSelectedItem() + "," + npcMoodBox.getSelectedItem() + ";";
                restoredLine += "meta:" + metaChapterBox.getSelectedItem() + "," + metaNextDayBox.getSelectedItem() + ";";
                restoredLine += "carma:" + carmaSpinner.getValue();

                lines.set(lineIndex, restoredLine);
            }
        }
    }

    private boolean saveAll() {
        if (scenario != null && lines != null) {
            config.setLastOpenPath(scenario);

            try (FileOutputStream fos = new FileOutputStream(scenario.toFile(), false);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 OutputStreamWriter osw = new OutputStreamWriter(bos, Charset.forName("UTF-8"))) {

                JIOM.dtoToFile(config);
                saveCurrentLine();

                for (String line : lines) {
                    osw.write(line);
                    osw.write(System.lineSeparator());
                }
                for (String line : answersArea.getText().split("\n")) {
                    osw.write(System.lineSeparator());
                    osw.write(line);
                }
                if (nfLink != null) {
                    osw.write("nf " + nfLink);
                }
                if (logicsLink != null) {
                    osw.write(logicsLink);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showConfirmDialog(this,
                        "Не удалось сохранить конфигурацию.", "Ошибка сохранения:",
                        JOptionPane.DEFAULT_OPTION);
                return false;
            }
        } else {
            JOptionPane.showConfirmDialog(this,
                    "Сценарий не выбран!", "Ошибка сохранения:",
                    JOptionPane.DEFAULT_OPTION);
            return false;
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        saveCurrentLine();

        if (saveAll()) {
            dispose();
            System.exit(0);
        } else {
            int req = JOptionPane.showConfirmDialog(this,
                    "Файл не был сохранен. Все равно выйти?" , "Внимание!" ,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (req == 0) {
                System.exit(0);
            }
        }
    }
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    private JFrame view;
    private final int lifeTime = 2;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComboBox<?> && !screenBox.getSelectedItem().equals("-")) {
            if (view != null && view.isVisible()) {
                view.dispose();
                view = null;
            }

            view = new JFrame() {
                @Override
                public void paint(Graphics g) {
                    super.paint(g);

                    try {
                        g.drawImage(ImageIO.read(new File(config.getScreensPath() + "/" + screenBox.getSelectedItem() + ".png")),
                                0,0,
                                getWidth(),getHeight(),
                                this);
                    } catch (Exception ignore) {
                    }
                }

                {
                    setFocusable(false);
                    setAutoRequestFocus(false);
                    setUndecorated(true);
                    setPreferredSize(new Dimension(450, 400));
                    pack();
                    setLocation(screenBox.getLocationOnScreen().x + screenBox.getBounds().width, screenBox.getLocationOnScreen().y);

                    addWindowListener(new WindowAdapter() {
                        Timer timer;
                        @Override
                        public void windowOpened(WindowEvent e) {
                            AtomicInteger time = new AtomicInteger(lifeTime);
                            timer = new Timer(1000, e1 -> {
                                time.getAndDecrement();
                                if (time.get() == 0) {
                                    timer.stop();
                                    dispose();
                                }
                            });
                            timer.start();
                        }

                        @Override
                        public void windowClosing(WindowEvent e) {
                            if (timer != null && timer.isRunning()) {
                                timer.stop();
                                dispose();
                            }
                        }
                    });
                }
            };
            view.setVisible(true);

            return;
        }

        switch (e.getActionCommand()) {
            case "next" -> {
                saveCurrentLine();
                if (lineIndex + 1 < lines.size()) {
                    lineIndex++;
                    setPage();
                }
            }
            case "prev" -> {
                saveCurrentLine();
                if (lineIndex > 0) {
                    lineIndex--;
                    setPage();
                }
            }
            case "open" -> {
                fch = new JFileChooser(config.getLastOpenPath() == null ? "./" : config.getLastOpenPath().getParent().toString());
                fch.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fch.setMultiSelectionEnabled(false);
                fch.setDialogTitle("Выбор сценария:");
                FileFilter filter = new FileNameExtensionFilter("Dejavu script", "dingo");
                fch.setFileFilter(filter);
                int result = fch.showOpenDialog(EditorFrame.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        scenario = fch.getSelectedFile().toPath();
                        reloadScript();
                        config.setLastOpenPath(scenario);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            case "save" -> saveAll();
//            case "meta" -> {
//                if (metaChapterBox.getSelectedItem().equals("-")) {
//                    metaNextDayBox.setSelectedIndex(0);
//                }
//            }
            case "npc" -> {
                if (npcNameBox.getSelectedItem().equals("-")) {
                    npcTypeBox.setSelectedIndex(0);
                    npcMoodBox.setSelectedIndex(0);
                }
            }
            case "resetLine" -> {
                setPage();
                reloadBtnsLinePanel();
                reloadAnswersBtns();
            }
            case "addLine" -> {
                lineIndex++;
                lines.add(lineIndex, "<new line>");
                setPage();
                saveCurrentLine();
                reloadBtnsLinePanel();
            }
            case "removeLine" -> {
                if (lines.size() > 1) {
                    lines.remove(lineIndex);
                    setPage();
                    reloadBtnsLinePanel();
                } else {
                    JOptionPane.showConfirmDialog(this,
                            "Это единственная строка", "Запрещено:",
                            JOptionPane.DEFAULT_OPTION);
                }
            }
            case "goNext" -> {
                try {
                    saveAll();
                    String nextScriptName = ((JButton) e.getSource()).getName().replace("\"", "");
                    if (Files.exists(Paths.get(config.getSciptsPath() + "\\" + nextScriptName + scriptExtension))) {
                        scenario = Paths.get(config.getSciptsPath() + "\\" + nextScriptName + scriptExtension);
                        reloadScript();
                    } else {
                        JOptionPane.showConfirmDialog(this,
                                "Проверь адрес скрипта.\nНе обнаружен скрипт " + (config.getSciptsPath() + "\\" + nextScriptName + scriptExtension),
                                "Не найдено:", JOptionPane.DEFAULT_OPTION);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            case "toLineQuick" -> {
                saveCurrentLine();
                lineIndex = Integer.parseInt(((JButton) e.getSource()).getName()) - 1;
                setPage();
            }
            default -> {}
        }
    }
}
