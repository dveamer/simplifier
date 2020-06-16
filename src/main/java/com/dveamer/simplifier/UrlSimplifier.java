package com.dveamer.simplifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class UrlSimplifier implements Simplifier {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<String> wordSet;
    private final Map<String, FullPath> fullPathMap;
    private final Map<SubPath, FullPath> lastSubPathMap;
    private final Set<SubPath> subPathSet;

    UrlSimplifier(Set<String> wordSet
            , Map<String, FullPath> fullPathMap
            , Map<SubPath, FullPath> lastSubPathMap
            , Set<SubPath> subPathSet) {
        this.wordSet = wordSet;
        this.fullPathMap = fullPathMap;
        this.lastSubPathMap = lastSubPathMap;
        this.subPathSet = subPathSet;

        if(logger.isDebugEnabled()
                && fullPathMap.size()<50
                && subPathSet.size()<100) {
            logger.debug("words : {}", wordSet);
            logger.debug("fullPathMap : {}", fullPathMap);
            logger.debug("lastSubPathMap : {}", lastSubPathMap);
            logger.debug("subPathSet : {}", subPathSet);
        }
    }

    @Override
    public String simplify(String path) {
        path = FullPath.validPath(path);

        if(fullPathMap.containsKey(path)) {
            return fullPathMap.get(path).getFullPath();
        }

        List<String> searchableWords = makeSearchableWords(path);
        String searchablePath = searchableWords.stream().collect(Collectors.joining(Constants.SYMBOL_PATH_DELIMITER));
        if(fullPathMap.containsKey(searchablePath)) {
            return fullPathMap.get(searchablePath).getFullPath();
        }

        logger.debug("searching.. {}", path);

        return find(searchableWords).getFullPath();
    }

    private List<String> makeSearchableWords(String path) {
        List<String> words = Arrays.asList(path.split(Constants.SYMBOL_PATH_DELIMITER));

        if(hasNoExtension(words)) {
            return makeSearchableWordsWithoutExtension(words);
        }

        String lastWordWithExtension = words.get(words.size()-1);
        int dotPosition = lastWordWithExtension.lastIndexOf(Constants.SYMBOL_DOT);
        String lastWord = lastWordWithExtension.substring(0, dotPosition);
        if(!wordSet.contains(lastWord)) {
            lastWord = Constants.SYMBOL_VARIABLE;
        }

        String extension = lastWordWithExtension.substring(dotPosition);
        if(!wordSet.contains(extension)) {
            extension = Constants.SYMBOL_DOT + Constants.SYMBOL_VARIABLE;
        }

        List<String> searchableWords = words.stream()
                .map(s->wordSet.contains(s)? s : Constants.SYMBOL_VARIABLE)
                .collect(Collectors.toList());

        searchableWords.set(words.size()-1, lastWord + extension);
        return searchableWords;
    }

    private boolean hasNoExtension(List<String> words) {
        return !words.get(words.size()-1).contains(Constants.SYMBOL_DOT);
    }

    private List<String> makeSearchableWordsWithoutExtension(List<String> words) {
        return words.stream()
                .map(s->wordSet.contains(s)? s : Constants.SYMBOL_VARIABLE)
                .collect(Collectors.toList());
    }

    private FullPath find(List<String> searchableWords) {
        SubPath firstSubPath = SubPath.createSubPath(searchableWords, 0);
        FullPath fullPath = find0(searchableWords, firstSubPath);
        if(fullPath!=null) {
            return fullPath;
        }

        throw new NoSuchElementException("Not Found : " + searchableWords);
    }

    private FullPath find(List<String> searchableWords, int index, SubPath previousSubPath) {
        SubPath subPath = SubPath.create(searchableWords, index, previousSubPath);
        if(subPath.isLast()) {
            return findFullPathOfLastSubPath(subPath);
        }

        if(subPathSet.contains(subPath)) {
            return find(searchableWords, index+1, subPath);
        }

        if(subPathSet.contains(subPath.variableSubPath())) {
            return find(searchableWords, index+1, subPath.variableSubPath());
        }
        throw new NoSuchElementException("Not Found : " + subPath + " / " + searchableWords);
    }

    private FullPath find0(List<String> searchableWords, SubPath subPath) {
        if(subPath.isLast()) {
            return findFullPathOfLastSubPath(subPath);
        }

        FullPath fullPath = null;
        if(subPathSet.contains(subPath)) {
            SubPath nextSubPath = SubPath.createSubPath(searchableWords, subPath.getIndex()+1);
            fullPath = find0(searchableWords, nextSubPath);
            if(fullPath!=null) {
                return fullPath;
            }
        }

        // TODO variable 처리할 때 extension 고려 필요.
        SubPath nextSubPath = SubPath.createVariableSubPath(searchableWords, subPath.getIndex()+1);
        return find0(searchableWords, nextSubPath);
    }

    private FullPath findFullPathOfLastSubPath(SubPath lastSubPath) {
        FullPath fullPath = lastSubPathMap.get(lastSubPath);
        if(fullPath != null) {
            return fullPath;
        }
        return lastSubPathMap.get(lastSubPath.variableSubPath());
    }


}
