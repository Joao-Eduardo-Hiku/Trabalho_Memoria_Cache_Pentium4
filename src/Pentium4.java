import javax.swing.*;
import java.awt.*;


public class Pentium4 extends JFrame {

    static final String[] PROG = {"MOV EAX, 5", "MOV EBX, 3", "ADD EAX, EBX", "MOV ECX, 10", "SUB ECX, 4", "IMUL EAX, ECX"};
    static final String[] LABEL = {"Guardar 5 em EAX", "Guardar 3 em EBX", "Somar EAX + EBX", "Guardar 10 em ECX", "Subtrair 4 de ECX", "Multiplicar EAX por ECX"};
    static final int[] DEST = {0, 1, 0, 2, 2, 0};
    static final int[] VAL = {5, 3, 8, 10, 6, 48};
    static final String[] REG = {"EAX", "EBX", "ECX", "EDX"};
    static final String[] NAME = {"1. BUSCA", "2. DECODIFICA", "3. ESCALONA", "4. EXECUTA", "5. RETIRA"};
    static final String[] DESC = {"Pega a instrução", "Traduz em passos", "Decide a ordem", "Faz a conta", "Guarda o resultado"};
    static final String[] WHAT = {
            "está sendo buscada na Trace Cache",
            "está sendo decodificada",
            "está no escalonador",
            "está sendo executada na ALU",
            "está sendo retirada: o resultado vai para o registrador"};
    static final Color[] SC = {new Color(0x60A5FA), new Color(0xA78BFA), new Color(0xFBBF24), new Color(0x4ADE80), new Color(0xF472B6)};
    static final Color BG = new Color(0x0F172A), PANEL = new Color(0x1E293B), FG = new Color(0xF1F5F9), MUTED = new Color(0x94A3B8);

    static class Board extends JPanel {
        int cycle = 0;
        double anim = 1.5;
        boolean pipeline = true;
        String speedText = "";

        Board() { setBackground(BG); }

        int start(int i) { return pipeline ? i + 1 : 5 * i + 1; }
        int total() { return start(PROG.length - 1) + 4; }
        int stageOf(int i, int c) { return c - start(i); }
        Color color(int i) { return Color.getHSBColor(i * 0.16f, 0.55f, 0.98f); }
        double ease(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }

        void text(Graphics2D g, String s, int x, int y, int maxW, int size, boolean bold, Color c, boolean center) {
            int sz = size; Font f; FontMetrics fm;
            do { f = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, sz--); fm = g.getFontMetrics(f); } while (fm.stringWidth(s) > maxW && sz > 8);
            g.setFont(f); g.setColor(c); g.drawString(s, center ? x - fm.stringWidth(s) / 2 : x, y);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight(), n = PROG.length;
            double a = ease(anim);

            text(g, "PENTIUM 4: a viagem de uma instrução pelo processador", W / 2, 34, W - 40, 28, true, FG, true);
            text(g, pipeline ? "MODO: COM PIPELINE (linha de montagem: várias instruções ao mesmo tempo)"
                            : "MODO: SEM PIPELINE (uma instrução por vez, o resto fica parado)", W / 2, 62, W - 40, 18, true,
                    pipeline ? new Color(0x4ADE80) : new Color(0xF87171), true);

            int mg = 30, gap = 44, bw = (W - 2 * mg - 4 * gap) / 5, by = 80, bh = 170;
            boolean[] busy = new boolean[5];
            for (int i = 0; i < n; i++) { int s = stageOf(i, cycle); if (s >= 0 && s <= 4) busy[s] = true; }
            for (int s = 0; s < 5; s++) {
                int x = mg + s * (bw + gap);
                g.setColor(busy[s] ? SC[s] : PANEL);
                g.fillRoundRect(x, by, bw, bh, 24, 24);
                g.setColor(SC[s]); g.setStroke(new BasicStroke(3f));
                g.drawRoundRect(x, by, bw, bh, 24, 24);
                text(g, NAME[s], x + bw / 2, by + 30, bw - 16, 20, true, busy[s] ? BG : SC[s], true);
                text(g, DESC[s], x + bw / 2, by + 52, bw - 16, 14, false, busy[s] ? BG : MUTED, true);
                if (s < 4) {
                    int ax = x + bw + 6, ay = by + bh / 2;
                    g.setColor(MUTED); g.setStroke(new BasicStroke(4f));
                    g.drawLine(ax, ay, ax + gap - 18, ay);
                    g.fillPolygon(new int[]{ax + gap - 8, ax + gap - 20, ax + gap - 20}, new int[]{ay, ay - 8, ay + 8}, 3);
                }
            }

            for (int i = 0; i < n; i++) {
                int s = stageOf(i, cycle), p = stageOf(i, cycle - 1);
                if (s < 0 || s > 5) continue;
                double fromX = (p >= 0 && p <= 4) ? mg + p * (bw + gap) : mg - (bw + gap);
                double toX = mg + Math.min(s, 5) * (bw + gap);
                double x = fromX + (toX - fromX) * a;
                float alpha = 1f;
                if (s == 5) alpha = (float) (1 - a);
                if (p < 0) alpha = (float) a;
                if (alpha <= 0.02f) continue;
                int cw = bw - 28, ch = 74, cy = by + 78;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(color(i)); g.fillRoundRect((int) x + 14, cy, cw, ch, 16, 16);
                g.setColor(BG); g.setStroke(new BasicStroke(2f)); g.drawRoundRect((int) x + 14, cy, cw, ch, 16, 16);
                text(g, "Instrução #" + (i + 1), (int) x + 14 + cw / 2, cy + 18, cw - 10, 13, false, new Color(0x334155), true);
                text(g, LABEL[i], (int) x + 14 + cw / 2, cy + 41, cw - 10, 18, true, BG, true);
                text(g, "(" + PROG[i] + ")", (int) x + 14 + cw / 2, cy + 62, cw - 10, 13, false, new Color(0x334155), true);
                g.setComposite(AlphaComposite.SrcOver);
            }

            int ry = by + bh + 22;
            int[] reg = new int[4];
            boolean[] flash = new boolean[4];
            for (int i = 0; i < n; i++) {
                int s = stageOf(i, cycle);
                if (s >= 4) { reg[DEST[i]] = VAL[i]; if (s == 4) flash[DEST[i]] = true; }
            }
            text(g, "REGISTRADORES (gavetas onde o processador guarda números)", mg, ry + 4, 600, 16, true, MUTED, false);
            int rw = 130;
            for (int r = 0; r < 4; r++) {
                int x = mg + r * (rw + 16), y = ry + 14;
                g.setColor(flash[r] ? new Color(0x16A34A) : PANEL); g.fillRoundRect(x, y, rw, 70, 16, 16);
                g.setColor(SC[3]); g.setStroke(new BasicStroke(2f)); g.drawRoundRect(x, y, rw, 70, 16, 16);
                text(g, REG[r], x + rw / 2, y + 22, rw, 16, true, MUTED, true);
                text(g, String.valueOf(reg[r]), x + rw / 2, y + 56, rw, 34, true, flash[r] ? Color.WHITE : new Color(0xFDE68A), true);
            }
            int done = 0;
            for (int i = 0; i < n; i++) if (stageOf(i, cycle) >= 5 || (stageOf(i, cycle) == 4 && anim >= 1)) done++;
            int px = mg + 4 * (rw + 16) + 30;
            text(g, "CICLO: " + cycle, px, ry + 34, 300, 30, true, FG, false);
            text(g, "Instruções prontas: " + done + " de " + n, px, ry + 58, 400, 20, false, FG, false);
            text(g, "Total neste modo: " + total() + " ciclos", px, ry + 80, 400, 20, true, pipeline ? new Color(0x4ADE80) : new Color(0xF87171), false);
            text(g, "Velocidade: " + speedText, px, ry + 102, 450, 17, true, new Color(0xFBBF24), false);

            int ty = ry + 124;
            text(g, "LINHA DO TEMPO  |  linha = instrução, coluna = ciclo  |  número na célula = fase: 1 Busca, 2 Decodifica, 3 Escalona, 4 Executa, 5 Retira", mg, ty + 4, W - 60, 16, true, MUTED, false);
            int leftW = 370, maxC = 30, cell = Math.min(44, (W - 2 * mg - leftW) / maxC), rh = 30;
            for (int c = 1; c <= total(); c++)
                text(g, String.valueOf(c), mg + leftW + (c - 1) * cell + cell / 2, ty + 26, cell, 12, true, c == cycle ? Color.WHITE : MUTED, true);
            for (int i = 0; i < n; i++) {
                int y = ty + 30 + i * (rh + 4);
                g.setColor(color(i)); g.fillRoundRect(mg, y, 14, rh, 6, 6);
                text(g, "#" + (i + 1) + "  " + LABEL[i], mg + 22, y + 21, 235, 16, true, FG, false);
                text(g, PROG[i], mg + 262, y + 21, leftW - 266, 13, false, MUTED, false);
                for (int c = 1; c <= cycle; c++) {
                    int s = stageOf(i, c);
                    if (s < 0 || s > 4) continue;
                    int x = mg + leftW + (c - 1) * cell;
                    g.setColor(SC[s]); g.fillRoundRect(x + 1, y, cell - 2, rh, 8, 8);
                    text(g, String.valueOf(s + 1), x + cell / 2, y + 21, cell - 4, 16, true, BG, true);
                }
            }
            if (cycle > 0) {
                g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2f));
                g.drawRect(mg + leftW + (cycle - 1) * cell, ty + 14, cell, n * (rh + 4) + 18);
            }

            int ey = ty + 30 + n * (rh + 4) + 22;
            if (cycle > 0) { g.setColor(new Color(0x1E3A5F)); g.fillRoundRect(mg, ey - 20, W - 2 * mg, H - ey - 6, 16, 16); }
            int ly = ey + 4;
            if (cycle == 0) {
                // sem mensagem inicial
            } else if (cycle >= total()) {
                text(g, "TERMINOU! Resultado final: EAX = 48.", mg + 14, ly + 6, W - 80, 22, true, new Color(0x86EFAC), false);
                text(g, "Com pipeline: " + (PROG.length + 4) + " ciclos  |  Sem pipeline: " + 5 * PROG.length + " ciclos.  Troque o modo e compare!", mg + 14, ly + 38, W - 80, 20, true, FG, false);
                text(g, "Isso acontece porque, na linha de montagem, cada fase trabalha numa instrução diferente ao mesmo tempo.", mg + 14, ly + 66, W - 80, 17, false, MUTED, false);
            } else {
                int line = 0;
                for (int i = 0; i < n; i++) {
                    int s = stageOf(i, cycle);
                    if (s < 0 || s > 4) continue;
                    g.setColor(color(i)); g.fillOval(mg + 14, ly + line * 26 - 8, 16, 16);
                    text(g, PROG[i] + "  " + WHAT[s], mg + 40, ly + line * 26 + 6, W - 110, 19, false, FG, false);
                    line++;
                }
                if (line == 0) text(g, "Nenhuma instrução em andamento.", mg + 14, ly + 6, W - 80, 19, false, FG, false);
            }
        }
    }

    final Board board = new Board();
    final javax.swing.Timer timer;
    boolean auto = false;
    final JSlider speed = new JSlider(1, 10, 5);
    final JLabel speedLbl = new JLabel();
    final JButton btnAuto = new JButton("Automático");

    Pentium4() {
        super("Pentium 4 - Simulação Simples");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(PANEL);
        JButton next = new JButton("Próximo ciclo"), reset = new JButton("Reiniciar"), mode = new JButton("Trocar: com / sem pipeline");
        speed.setMajorTickSpacing(1); speed.setPaintTicks(true); speed.setSnapToTicks(true);
        speed.setPreferredSize(new Dimension(220, 40));
        java.util.Hashtable<Integer, JLabel> lt = new java.util.Hashtable<>();
        lt.put(1, new JLabel("Lenta")); lt.put(5, new JLabel("Média")); lt.put(10, new JLabel("Rápida"));
        for (JLabel x : lt.values()) x.setForeground(FG);
        speed.setLabelTable(lt); speed.setPaintLabels(true);
        for (JButton b : new JButton[]{next, btnAuto, reset, mode}) { b.setFont(b.getFont().deriveFont(Font.BOLD, 16f)); bar.add(b); }
        JLabel l = new JLabel("Velocidade:"); l.setForeground(FG); l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        bar.add(l); bar.add(speed); speed.setOpaque(false);
        speedLbl.setForeground(new Color(0xFBBF24)); speedLbl.setFont(speedLbl.getFont().deriveFont(Font.BOLD, 15f)); bar.add(speedLbl);
        speed.addChangeListener(e -> updateSpeed());
        updateSpeed();
        add(bar, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);

        next.addActionListener(e -> { setAuto(false); step(); });
        btnAuto.addActionListener(e -> setAuto(!auto));
        reset.addActionListener(e -> { setAuto(false); board.cycle = 0; board.anim = 1.5; board.repaint(); });
        mode.addActionListener(e -> { setAuto(false); board.pipeline = !board.pipeline; board.cycle = 0; board.anim = 1.5; board.repaint(); });

        timer = new javax.swing.Timer(16, e -> {
            double dur = durMs();
            if (board.anim < 1.4) board.anim += 16.0 / dur * 1.4;
            if (auto && board.anim >= 1.4) step();
            board.repaint();
        });
        timer.start();
        setSize(1280, 900);
        setLocationRelativeTo(null);
    }

    int durMs() { return 1700 - speed.getValue() * 150; } // tempo de cada ciclo, em ms

    void updateSpeed() {
        int v = speed.getValue();
        String t = (v <= 3 ? "Lenta" : v <= 7 ? "Média" : "Rápida") + " (" + String.format("%.2f", durMs() / 1000.0) + " s por ciclo)";
        speedLbl.setText(t);
        board.speedText = t;
        board.repaint();
    }

    void step() {
        if (board.cycle >= board.total()) { setAuto(false); return; }
        board.cycle++;
        board.anim = 0;
    }

    void setAuto(boolean on) {
        auto = on;
        btnAuto.setText(on ? "Pausar" : "Automático");
        if (on && board.cycle >= board.total()) { board.cycle = 0; board.anim = 1.5; }
    }

}