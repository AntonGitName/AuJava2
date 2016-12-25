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
    private static final int minSz = 4;
    private static final int mX = 3;
    private static final int mY = 3;
    private static final int dx = 2;
    private static final int dy = 2;
    private final int num;
    @Setter
    private Set<Integer> blocks;

    public BlockPanel(Set<Integer> blocks, int num) {
        this.blocks = blocks;
        this.num = num;
    }

    private static int roundUpDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        final Color oldColor = g.getColor();


        final int aH = getHeight() - 2 * mY;
        final int aW = getWidth() - 2 * mX;

        int perLine = num;
        int rows = num / perLine;
        int stepX = roundUpDiv(aW - (perLine - 1) * dx, perLine);
        int stepY = roundUpDiv(aH - (rows - 1) * dy, rows);

        while (stepX < minSz && stepY > minSz) {
            perLine /= 2;
            rows = num / perLine;
            stepX = roundUpDiv(aW - (perLine - 1) * dx, perLine);
            stepY = (aH - (rows - 1) * dy) / rows;
        }
        if (stepX >= minSz && stepY >= minSz) {
            for (int i = 0; i < num; ++i) {
                final int row = i / perLine;
                final int col = i % perLine;
                final int x = mX + (stepX + dx) * col;
                final int y = mY + (stepY + dy) * row;
                final int blockWidth = Math.max(0, Math.min(stepX, aW + mX - x));
                final int blockHeight = stepY;
                g.setColor(blocks.contains(i) ? Color.GREEN : Color.RED);
                g.fillRect(x, y, blockWidth, blockHeight);
            }
        } else {
            final int split = aW * blocks.size() / num;
            g.setColor(Color.GREEN);
            g.fillRect(mX, mY, split, aH);
            g.setColor(Color.RED);
            g.fillRect(mX + split, mY, aW - split, aH);
        }

        g.setColor(oldColor);
    }
}
