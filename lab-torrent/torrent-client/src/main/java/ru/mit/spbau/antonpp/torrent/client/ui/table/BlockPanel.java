package ru.mit.spbau.antonpp.torrent.client.ui.table;

import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * @author antonpp
 * @since 14/12/2016
 */
public final class BlockPanel extends JPanel {
    private final int num;
    @Setter
    private Set<Integer> blocks;

    public BlockPanel(Set<Integer> blocks, int num) {
        this.blocks = blocks;
        this.num = num;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        final double w = getWidth();
        final double h = getHeight();
        final int mX = 3;
        final int mY = 3;
        final int dx = 2;

        final int step = (int) ((w - 2 * mX - (num - 1) * dx) / num);
        final Color oldColor = g.getColor();

        final int blockWidth = Math.max(1, step);

        for (int i = 0; i < num; ++i) {
            g.setColor(blocks.contains(i) ? Color.GREEN : Color.RED);
            int x = mX + (step + dx) * i;
            int bw = Math.max(0, Math.min(blockWidth, (int) w - mX - x));
            g.fillRect(x, mY, bw, (int) (h - 2 * mY));
        }

        g.setColor(oldColor);
    }
}
