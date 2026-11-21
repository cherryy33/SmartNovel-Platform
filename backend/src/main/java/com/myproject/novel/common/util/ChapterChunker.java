package com.myproject.novel.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节正文分块工具
 * <p>
 * RAG 的第一步是把长文本切成适合向量化的片段（Chunk）。本工具优先按中文标点/换行断句，
 * 避免把一个句子拦腰截断；相邻分块之间保留一定长度的重叠文本，防止关键信息正好落在切分点上。
 */
public final class ChapterChunker {

    /** 以句末标点或换行作为断句依据，保留标点 */
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^。！？!?；;\\n]+[。！？!?；;\\n]?");

    private ChapterChunker() {
        throw new IllegalStateException("工具类不允许实例化");
    }

    /**
     * 将章节正文切分为若干文本块
     *
     * @param content   章节正文
     * @param chunkSize 单个分块的最大字符数，小于 100 时按 100 处理
     * @param overlap   相邻分块的重叠字符数，最大不超过分块大小的一半
     * @return 分块列表，正文为空时返回空列表
     */
    public static List<String> chunk(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }

        int size = Math.max(100, chunkSize);
        int safeOverlap = Math.max(0, Math.min(overlap, size / 2));

        String text = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        StringBuilder current = new StringBuilder(size + safeOverlap);

        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().strip();
            if (sentence.isEmpty()) {
                continue;
            }
            for (String piece : hardSplit(sentence, size)) {
                if (current.length() > 0 && current.length() + piece.length() > size) {
                    String finished = current.toString().strip();
                    if (finished.length() > safeOverlap) {
                        chunks.add(finished);
                    }
                    String tail = "";
                    if (safeOverlap > 0) {
                        tail = finished.length() > safeOverlap
                            ? finished.substring(finished.length() - safeOverlap)
                            : finished;
                    }
                    current.setLength(0);
                    current.append(tail);
                }
                current.append(piece);
            }
        }

        String last = current.toString().strip();
        if (!last.isEmpty() && (chunks.isEmpty() || last.length() > safeOverlap)) {
            chunks.add(last);
        }
        return chunks;
    }

    /** 对超长句子（如没有标点的段落）做硬切分 */
    private static List<String> hardSplit(String sentence, int size) {
        List<String> pieces = new ArrayList<>();
        if (sentence.length() <= size) {
            pieces.add(sentence);
            return pieces;
        }
        for (int start = 0; start < sentence.length(); start += size) {
            pieces.add(sentence.substring(start, Math.min(sentence.length(), start + size)));
        }
        return pieces;
    }
}
