import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Stack; // Import Stack

public class DrawingApp extends JFrame {
    private DrawArea drawArea;
    private Choice brushSizeChoice;
    private Choice toolChoice;
    private Button colorChooserButton;
    private Color selectedColor = Color.BLACK;

    public DrawingApp() {
        setTitle("Simple Drawing App");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        drawArea = new DrawArea();
        add(drawArea, BorderLayout.CENTER);

        // Top panel
        JPanel topPanel = new JPanel();

        // Color chooser
        colorChooserButton = new Button("Color Picker");
        colorChooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color newColor = JColorChooser.showDialog(null, "Choose a color", selectedColor);
                if (newColor != null) {
                    selectedColor = newColor;
                    drawArea.setCurrentColor(selectedColor);
                }
            }
        });

        // Brush size
        brushSizeChoice = new Choice();
        brushSizeChoice.add("Small");
        brushSizeChoice.add("Medium");
        brushSizeChoice.add("Large");

        brushSizeChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateBrushSize();
            }
        });

        // Tool selector
        toolChoice = new Choice();
        toolChoice.add("Pencil");
        toolChoice.add("Eraser");
        toolChoice.add("Line");
        toolChoice.add("Rectangle");
        toolChoice.add("Circle");

        toolChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                drawArea.setTool(toolChoice.getSelectedItem());
            }
        });

        // Undo button
        Button undoButton = new Button("Undo");
        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawArea.undo();
            }
        });

        // Redo button
        Button redoButton = new Button("Redo");
        redoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawArea.redo();
            }
        });

        // Clear button
        Button clearButton = new Button("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawArea.clear();
            }
        });

        // Save button
        Button saveButton = new Button("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawArea.saveImage();
            }
        });

        topPanel.add(colorChooserButton);
        topPanel.add(new Label("Brush:"));
        topPanel.add(brushSizeChoice);
        topPanel.add(new Label("Tool:"));
        topPanel.add(toolChoice);
        topPanel.add(undoButton);
        topPanel.add(redoButton);
        topPanel.add(clearButton);
        topPanel.add(saveButton);

        add(topPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    private void updateBrushSize() {
        String selectedSize = brushSizeChoice.getSelectedItem();
        if (selectedSize.equals("Small")) {
            drawArea.setBrushSize(2);
        } else if (selectedSize.equals("Medium")) {
            drawArea.setBrushSize(6);
        } else if (selectedSize.equals("Large")) {
            drawArea.setBrushSize(12);
        }
    }

    public static void main(String[] args) {
        new DrawingApp();
    }
}

class DrawArea extends JPanel {
    private int prevX, prevY, startX, startY;
    private Image image;
    private Graphics2D g2;
    private Color currentColor = Color.BLACK;
    private int brushSize = 2;
    private String currentTool = "Pencil";

    // Undo/Redo Stacks
    private Stack<Image> undoStack = new Stack<Image>();
    private Stack<Image> redoStack = new Stack<Image>();

    public DrawArea() {
        setDoubleBuffered(false);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                prevX = e.getX();
                prevY = e.getY();
                startX = prevX;
                startY = prevY;
                saveStateForUndo(); // SAVE SNAPSHOT before new stroke
            }

            public void mouseReleased(MouseEvent e) {
                if (currentTool.equals("Line") || currentTool.equals("Rectangle") || currentTool.equals("Circle")) {
                    int x = e.getX();
                    int y = e.getY();
                    g2.setColor(currentColor);
                    g2.setStroke(new BasicStroke(brushSize));
                    if (currentTool.equals("Line")) {
                        g2.drawLine(startX, startY, x, y);
                    } else if (currentTool.equals("Rectangle")) {
                        g2.drawRect(Math.min(startX, x), Math.min(startY, y),
                                Math.abs(x - startX), Math.abs(y - startY));
                    } else if (currentTool.equals("Circle")) {
                        g2.drawOval(Math.min(startX, x), Math.min(startY, y),
                                Math.abs(x - startX), Math.abs(y - startY));
                    }
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                if (g2 != null) {
                    g2.setStroke(new BasicStroke(brushSize));

                    if (currentTool.equals("Pencil")) {
                        g2.setColor(currentColor);
                        g2.drawLine(prevX, prevY, x, y);
                    } else if (currentTool.equals("Eraser")) {
                        g2.setColor(Color.WHITE);
                        g2.drawLine(prevX, prevY, x, y);
                    }

                    repaint();
                    prevX = x;
                    prevY = y;
                }
            }
        });
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    public void setTool(String tool) {
        this.currentTool = tool;
    }

    protected void paintComponent(Graphics g) {
        if (image == null) {
            image = createImage(getSize().width, getSize().height);
            g2 = (Graphics2D) image.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            clear();
        }

        g.drawImage(image, 0, 0, null);
    }

    public void clear() {
        g2.setPaint(Color.white);
        g2.fillRect(0, 0, getSize().width, getSize().height);
        g2.setPaint(currentColor);
        repaint();
    }

    // Save the current state for Undo/Redo
    private void saveStateForUndo() {
        if (image != null) {
            Image snapshot = createImage(getWidth(), getHeight());
            Graphics g = snapshot.getGraphics();
            g.drawImage(image, 0, 0, null);
            undoStack.push(snapshot);
            redoStack.clear(); // once you draw, redo stack is cleared
        }
    }

    // Undo Action
    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(image);
            image = undoStack.pop();
            g2 = (Graphics2D) image.getGraphics();
            repaint();
        }
    }

    // Redo Action
    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(image);
            image = redoStack.pop();
            g2 = (Graphics2D) image.getGraphics();
            repaint();
        }
    }

    public void saveImage() {
        try {
            int w = getWidth();
            int h = getHeight();
            BufferedImage bImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bImage.createGraphics();
            this.paint(g2d);
            g2d.dispose();

            File outputfile = new File("drawing_" + System.currentTimeMillis() + ".png");
            ImageIO.write(bImage, "png", outputfile);
            System.out.println("Saved as: " + outputfile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error saving image: " + e.getMessage());
        }
    }
}
