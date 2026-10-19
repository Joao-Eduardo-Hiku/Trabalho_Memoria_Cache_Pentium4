import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Pentium4 extends JFrame {

    static final Color BG = new Color(0x0F172A), PANEL = new Color(0x1E293B), FG = new Color(0xF1F5F9),
            MUTED = new Color(0xCBD5E1), OK = new Color(0x166534), ERRO = new Color(0x991B1B),
            VAZIA = new Color(0x0B1220), HIT = new Color(0x4ADE80), NOVA = new Color(0x38BDF8);
    static final Color[] LC = {new Color(0x60A5FA), new Color(0xFBBF24), new Color(0xA78BFA)};

    static class Cache {
        final String nome, info;
        final int sets, ways, lineBits, idxBits;
        final int[][] tag;
        final long[][] uso;
        long t;
        int acertos, erros, ultSet, ultVia;
        boolean ultAcerto;
        String ultimo = "";

        Cache(String nome, String info, int sets, int ways, int lineBits) {
            this.nome = nome; this.info = info; this.sets = sets; this.ways = ways; this.lineBits = lineBits;
            idxBits = Integer.numberOfTrailingZeros(sets);
            tag = new int[sets][ways]; uso = new long[sets][ways];
            reset();
        }

        void reset() {
            for (int[] l : tag) Arrays.fill(l, -1);
            for (long[] l : uso) Arrays.fill(l, 0);
            t = 0; acertos = 0; erros = 0; ultSet = -1; ultVia = -1; ultimo = "";
        }

        boolean acessa(int addr) {
            int s = (addr >>> lineBits) & (sets - 1), tg = addr >>> (lineBits + idxBits);
            t++; ultSet = s;
            for (int w = 0; w < ways; w++)
                if (tag[s][w] == tg) { uso[s][w] = t; acertos++; ultVia = w; ultAcerto = true; return true; }
            int v = 0;                                   // via livre ou menos usada (LRU)
            for (int w = 1; w < ways; w++) if (uso[s][w] < uso[s][v]) v = w;
            tag[s][v] = tg; uso[s][v] = t; erros++; ultVia = v; ultAcerto = false;
            return false;
        }

        String divide(int a) {
            return String.format("offset %d (%d bits) · índice %d (%d bits) · tag 0x%X (%d bits)",
                    a & ((1 << lineBits) - 1), lineBits, (a >>> lineBits) & (sets - 1), idxBits,
                    a >>> (lineBits + idxBits), 32 - lineBits - idxBits);
        }
    }

    static class Painel extends JPanel {
        final Cache tc = new Cache("Trace Cache", "12K µops · 8 vias · 256 conjuntos · 6 µops por linha", 256, 8, 5);
        final Cache l1 = new Cache("L1 de dados", "8 KB · 4 vias · 32 conjuntos · linha de 64 B · write-through", 32, 4, 6);
        final Cache l2 = new Cache("L2 unificada", "512 KB · 8 vias · 512 conjuntos · linha de 128 B · write-back", 512, 8, 7);
        final Cache[] cs = {tc, l1, l2};
        static final int[] COLS = {32, 8, 64};          // colunas do mapa de conjuntos
        final int[] estado = new int[4];                // 0 neutro, 1 acerto, 2 erro (índice 3 = RAM)
        final List<int[]> passos = new ArrayList<>();
        int mostrados = 0, ramAcessos = 0;
        boolean houve = false;
        final javax.swing.Timer timer = new javax.swing.Timer(500, e -> proximo());

        Painel() { setBackground(BG); setPreferredSize(new Dimension(1200, 640)); }

        void reset() {
            timer.stop();
            for (Cache c : cs) c.reset();
            Arrays.fill(estado, 0); passos.clear(); ramAcessos = 0; houve = false;
            repaint();
        }

        void acesso(boolean busca, int addr) {
            Cache primeira = busca ? tc : l1;
            for (Cache c : cs) c.ultimo = "";
            passos.clear(); Arrays.fill(estado, 0); mostrados = 0; houve = true;
            primeira.ultimo = primeira.divide(addr);
            boolean h1 = primeira.acessa(addr);
            passos.add(new int[]{busca ? 0 : 1, h1 ? 1 : 2});
            if (!h1) {
                l2.ultimo = l2.divide(addr);
                boolean h2 = l2.acessa(addr);
                passos.add(new int[]{2, h2 ? 1 : 2});
                if (!h2) { passos.add(new int[]{3, 1}); ramAcessos++; }
            }
            timer.restart();
            repaint();
        }

        void proximo() {
            if (mostrados < passos.size()) { int[] p = passos.get(mostrados++); estado[p[0]] = p[1]; }
            if (mostrados >= passos.size()) timer.stop();
            repaint();
        }

        void txt(Graphics2D g, String s, int cx, int y, int maxW, int size, boolean bold, Color c) {
            int sz = Math.max(size, 9); Font f; FontMetrics fm;
            do { f = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, sz--); fm = g.getFontMetrics(f); }
            while (fm.stringWidth(s) > maxW && sz > 8);
            g.setFont(f); g.setColor(c); g.drawString(s, cx - fm.stringWidth(s) / 2, y);
        }

        void seta(Graphics2D g, int x1, int y1, int x2, int y2) {
            g.setColor(Color.GRAY); g.setStroke(new BasicStroke(3f));
            g.drawLine(x1, y1, x2, y2);
            double a = Math.atan2(y2 - y1, x2 - x1);
            g.fillPolygon(new int[]{x2, (int) (x2 - 14 * Math.cos(a - 0.45)), (int) (x2 - 14 * Math.cos(a + 0.45))},
                    new int[]{y2, (int) (y2 - 14 * Math.sin(a - 0.45)), (int) (y2 - 14 * Math.sin(a + 0.45))}, 3);
        }

        void fundo(Graphics2D g, int i, Rectangle r, Color borda) {
            g.setColor(estado[i] == 1 ? OK : estado[i] == 2 ? ERRO : PANEL);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 28, 28);
            g.setColor(borda); g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 28, 28);
        }

        void caixaCache(Graphics2D g, int i, Rectangle r) {
            Cache c = cs[i];
            fundo(g, i, r, LC[i]);
            int cx = r.x + r.width / 2, mw = r.width - 30, ts = Math.max(14, Math.min(30, r.height / 11));
            String suf = estado[i] == 1 ? " · ACERTO" : estado[i] == 2 ? " · ERRO" : "";
            String taxa = c.acertos + c.erros == 0 ? "-" : String.format("%.1f%%", 100.0 * c.acertos / (c.acertos + c.erros));
            txt(g, c.nome + suf, cx, r.y + (int) (r.height * 0.11), mw, ts, true, FG);
            txt(g, c.info, cx, r.y + (int) (r.height * 0.18), mw, ts * 3 / 5, false, MUTED);
            txt(g, "Acertos " + c.acertos + " · Erros " + c.erros + " · Taxa de acerto " + taxa,
                    cx, r.y + (int) (r.height * 0.26), mw, ts * 2 / 3, true, FG);
            mapa(g, i, r.x + (int) (r.width * 0.06), r.y + (int) (r.height * 0.31), (int) (r.width * 0.88), (int) (r.height * 0.48));
            String det = !c.ultimo.isEmpty() ? c.ultimo : houve ? "Não consultada neste acesso" : "Nenhum acesso ainda";
            txt(g, det, cx, r.y + (int) (r.height * 0.89), mw, ts * 2 / 3, false, FG);
        }

        void mapa(Graphics2D g, int i, int x, int y, int w, int h) {
            Cache c = cs[i];
            int cols = COLS[i], rows = (c.sets + cols - 1) / cols;
            double cw = (double) w / cols, ch = (double) h / rows;
            g.setColor(VAZIA); g.fillRect(x - 3, y - 3, w + 6, h + 6);
            for (int s = 0; s < c.sets; s++) {
                double cx = x + (s % cols) * cw, cy = y + (s / cols) * ch;
                for (int v = 0; v < c.ways; v++) {
                    double sw = cw / c.ways;
                    g.setColor(c.tag[s][v] < 0 ? new Color(0x16213A) : LC[i]);
                    g.fill(new Rectangle2D.Double(cx + sw * v, cy, Math.max(1, sw - (sw > 3 ? 1 : 0)), Math.max(1, ch - (ch > 4 ? 2 : 0))));
                }
            }
            if (estado[i] != 0 && c.ultSet >= 0) {   // destaca o conjunto e a via do último acesso
                int s = c.ultSet;
                double cx = x + (s % cols) * cw, cy = y + (s / cols) * ch, sw = cw / c.ways;
                g.setColor(c.ultAcerto ? HIT : NOVA);
                g.fill(new Rectangle2D.Double(cx + sw * c.ultVia, cy, Math.max(2, sw), Math.max(1, ch - 2)));
                g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2f));
                g.draw(new Rectangle2D.Double(cx - 1, cy - 1, cw + 1, ch));
            }
        }

        void caixaRam(Graphics2D g, Rectangle r) {
            fundo(g, 3, r, Color.LIGHT_GRAY);
            int cx = r.x + r.width / 2, mw = r.width - 30, ts = Math.max(14, Math.min(30, r.height / 7));
            txt(g, "Memória RAM" + (estado[3] == 1 ? " · LIDA" : ""), cx, r.y + (int) (r.height * 0.32), mw, ts, true, FG);
            txt(g, "via barramento FSB", cx, r.y + (int) (r.height * 0.50), mw, ts * 3 / 5, false, MUTED);
            txt(g, "Leituras: " + ramAcessos, cx, r.y + (int) (r.height * 0.70), mw, ts * 2 / 3, true, FG);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight(), m = 30, gap = 70, gv = 30;
            int usable = W - 2 * m - 2 * gap, wL = (int) (usable * 0.38), wM = (int) (usable * 0.42), wR = usable - wL - wM;
            int hh = H - 2 * m - 20, hL = (hh - gv) / 2;
            Rectangle rTc = new Rectangle(m, m, wL, hL), rL1 = new Rectangle(m, m + hL + gv, wL, hL),
                    rL2 = new Rectangle(m + wL + gap, m, wM, hh), rRam = new Rectangle(m + wL + gap + wM + gap, m + hh / 4, wR, hh / 2);

            seta(g, rTc.x + rTc.width, rTc.y + rTc.height / 2, rL2.x, rTc.y + rTc.height / 2);
            seta(g, rL1.x + rL1.width, rL1.y + rL1.height / 2, rL2.x, rL1.y + rL1.height / 2);
            seta(g, rL2.x + rL2.width, m + hh / 2, rRam.x, m + hh / 2);
            caixaCache(g, 0, rTc); caixaCache(g, 1, rL1); caixaCache(g, 2, rL2); caixaRam(g, rRam);

            // legenda
            Color[] lc = {new Color(0x16213A), LC[1], HIT, NOVA};
            String[] lt = {"via vazia", "via ocupada", "acerto (via lida)", "linha nova após erro (via gravada)"};
            int lx = m, ly = H - 12;
            for (int k = 0; k < 4; k++) {
                g.setColor(lc[k]); g.fillRect(lx, ly - 11, 14, 14);
                g.setColor(Color.GRAY); g.setStroke(new BasicStroke(1f)); g.drawRect(lx, ly - 11, 14, 14);
                g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(MUTED); g.drawString(lt[k], lx + 20, ly);
                lx += 40 + g.getFontMetrics().stringWidth(lt[k]);
            }
            g.drawString("Cada bloco do mapa é um conjunto; cada faixa dentro dele é uma via.", lx + 20, ly);
        }
    }

    final Painel painel = new Painel();
    final JTextField campo = new JTextField("0x00100000", 10);

    Pentium4() {
        super("Cache do Pentium 4");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JButton bLer = new JButton("Ler dado"), bBuscar = new JButton("Buscar instrução"),
                bAleat = new JButton("Endereço aleatório"), bReset = new JButton("Reiniciar");
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.add(new JLabel("Endereço (hex):")); barra.add(campo);
        barra.add(bLer); barra.add(bBuscar); barra.add(bAleat); barra.add(bReset);
        add(barra, BorderLayout.NORTH);
        add(painel, BorderLayout.CENTER);

        bLer.addActionListener(e -> acesso(false));
        bBuscar.addActionListener(e -> acesso(true));
        campo.addActionListener(e -> acesso(false));
        bAleat.addActionListener(e -> campo.setText(String.format("0x%08X", new Random().nextInt(1 << 22) & ~3)));
        bReset.addActionListener(e -> painel.reset());
        setSize(1280, 760);
        setLocationRelativeTo(null);
    }

    void acesso(boolean busca) {
        try {
            int addr = (int) Long.parseLong(campo.getText().trim().replaceFirst("^0[xX]", ""), 16);
            painel.acesso(busca, addr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Endereço inválido: use hexadecimal (ex.: 0x00100040).");
        }
    }

}