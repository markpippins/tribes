package com.angrysurfer.core.api.db;

import java.util.List;
import java.util.Set;

import javax.sound.midi.Instrument;

import com.angrysurfer.core.model.ControlCode;
import com.angrysurfer.core.model.ControlCodeCaption;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.feature.Pad;

@Deprecated
public interface Database {

    FindAll<ControlCodeCaption> getCaptionFindAll();

    FindOne<ControlCodeCaption> getCaptionFindOne();

    Save<ControlCodeCaption> getCaptionSaver();

    Delete<ControlCodeCaption> getCaptionDeleter();

    FindAll<ControlCode> getControlCodeFindAll();

    FindOne<ControlCode> getControlCodeFindOne();

    Save<ControlCode> getControlCodeSaver();

    Delete<ControlCode> getControlCodeDeleter();

    FindAll<Instrument> getInstrumentFindAll();

    FindOne<Instrument> getInstrumentFindOne();

    Save<Instrument> getInstrumentSaver();

    Delete<Instrument> getInstrumentDeleter();

    FindAll<Pad> getPadFindAll();

    FindOne<Pad> getPadFindOne();

    Save<Pad> getPadSaver();

    Delete<Pad> getPadDeleter();

    FindAll<Pattern> getPatternFindAll();

    FindOne<Pattern> getPatternFindOne();

    FindSet<Pattern> getSongPatternFinder();

    Save<Pattern> getPatternSaver();

    Delete<Pattern> getPatternDeleter();

    FindAll<Rule> getRuleFindAll();

    FindOne<Rule> getRuleFindOne();

    FindSet<Rule> getPlayerRuleFindSet();

    Save<Rule> getRuleSaver();

    Delete<Rule> getRuleDeleter();

    FindAll<Song> getSongFindAll();

    FindOne<Song> getSongFindOne();

    Save<Song> getSongSaver();

    Delete<Song> getSongDeleter();

    Next<Song> getSongForward();

    Prior<Song> getSongBack();

    Max<Song> getSongMax();

    Min<Song> getSongMin();

    FindAll<Step> getStepFindAll();

    FindOne<Step> getStepFindOne();

    FindSet<Step> getPatternStepFinder();

    Save<Step> getStepSaver();

    Delete<Step> getStepDeleter();

    FindAll<Strike> getStrikeFindAll();

    FindOne<Strike> getStrikeFindOne();

    FindSet<Strike> getTickerStrikeFinder();

    Save<Player> getStrikeSaver();

    Delete<Player> getStrikeDeleter();

    FindAll<Session> getTickerFindAll();

    FindOne<Session> getTickerFindOne();

    Save<Session> getTickerSaver();

    Delete<Session> getTickerDeleter();

    Next<Session> getTickerForward();

    Prior<Session> getTickerBack();

    Max<Session> getTickerMax();

    Min<Session> getTickerMin();

    void setCaptionFindAll(FindAll<ControlCodeCaption> captionFindAll);

    void setCaptionFindOne(FindOne<ControlCodeCaption> captionFindOne);

    void setCaptionSaver(Save<ControlCodeCaption> captionSaver);

    void setCaptionDeleter(Delete<ControlCodeCaption> captionDeleter);

    void setControlCodeFindAll(FindAll<ControlCode> controlCodeFindAll);

    void setControlCodeFindOne(FindOne<ControlCode> controlCodeFindOne);

    void setControlCodeSaver(Save<ControlCode> controlCodeSaver);

    void setControlCodeDeleter(Delete<ControlCode> controlCodeDeleter);

    void setInstrumentFindAll(FindAll<Instrument> instrumentFindAll);

    void setInstrumentFindOne(FindOne<Instrument> instrumentFindOne);

    void setInstrumentSaver(Save<Instrument> instrumentSaver);

    void setInstrumentDeleter(Delete<Instrument> instrumentDeleter);

    void setPadFindAll(FindAll<Pad> padFindAll);

    void setPadFindOne(FindOne<Pad> padFindOne);

    void setPadSaver(Save<Pad> padSaver);

    void setPadDeleter(Delete<Pad> padDeleter);

    void setPatternFindAll(FindAll<Pattern> patternFindAll);

    void setPatternFindOne(FindOne<Pattern> patternFindOne);

    void setSongPatternFinder(FindSet<Pattern> songPatternFinder);

    void setPatternSaver(Save<Pattern> patternSaver);

    void setPatternDeleter(Delete<Pattern> patternDeleter);

    void setRuleFindAll(FindAll<Rule> ruleFindAll);

    void setRuleFindOne(FindOne<Rule> ruleFindOne);

    void setPlayerRuleFindSet(FindSet<Rule> playerRuleFindSet);

    void setRuleSaver(Save<Rule> ruleSaver);

    void setRuleDeleter(Delete<Rule> ruleDeleter);

    void setSongFindAll(FindAll<Song> songFindAll);

    void setSongFindOne(FindOne<Song> songFindOne);

    void setSongSaver(Save<Song> songSaver);

    void setSongDeleter(Delete<Song> songDeleter);

    void setSongForward(Next<Song> songForward);

    void setSongBack(Prior<Song> songBack);

    void setSongMax(Max<Song> songMax);

    void setSongMin(Min<Song> songMin);

    void setStepFindAll(FindAll<Step> stepFindAll);

    void setStepFindOne(FindOne<Step> stepFindOne);

    void setPatternStepFinder(FindSet<Step> patternStepFinder);

    void setStepSaver(Save<Step> stepSaver);

    void setStepDeleter(Delete<Step> stepDeleter);

    void setStrikeFindAll(FindAll<Strike> strikeFindAll);

    void setStrikeFindOne(FindOne<Strike> strikeFindOne);

    void setTickerStrikeFinder(FindSet<Strike> tickerStrikeFinder);

    void setStrikeSaver(Save<Player> strikeSaver);

    void setStrikeDeleter(Delete<Player> strikeDeleter);

    void setTickerFindAll(FindAll<Session> tickerFindAll);

    void setTickerFindOne(FindOne<Session> tickerFindOne);

    void setTickerSaver(Save<Session> tickerSaver);

    void setTickerDeleter(Delete<Session> tickerDeleter);

    void setTickerForward(Next<Session> tickerForward);

    void setTickerBack(Prior<Session> tickerBack);

    void setTickerMax(Max<Session> tickerMax);

    void setTickerMin(Min<Session> tickerMin);

    void clearDatabase();

    // Caption related public methods
    ControlCodeCaption findCaptionById(Long id);

    List<ControlCodeCaption> findAllCaptions();

    ControlCodeCaption saveCaption(ControlCodeCaption caption);

    void deleteCaption(ControlCodeCaption caption);

    // ControlCode related public methods
    ControlCode findControlCodeById(Long id);

    List<ControlCode> findAllControlCodes();

    ControlCode saveControlCode(ControlCode controlCode);

    void deleteControlCode(ControlCode controlCode);

    // Instrument related public methods
    Instrument findInstrumentById(Long id);

    List<Instrument> findAllInstruments();

    Instrument saveInstrument(Instrument instrument);

    void deleteInstrument(Instrument instrument);

    // Pad related public methods
    Pad findPadById(Long id);

    List<Pad> findAllPads();

    Pad savePad(Pad pad);

    void deletePad(Pad pad);

    // Pattern related public methods
    Pattern findPatternById(Long id);

    Set<Pattern> findPatternBySongId(Long id);

    List<Pattern> findAllPatterns();

    Pattern savePattern(Pattern pattern);

    void deletePattern(Pattern pattern);

    // Rule related public methods
    Rule findRuleById(Long id);

    Set<Rule> findRulesByPlayerId(Long playerId);

    Rule saveRule(Rule rule);

    void deleteRule(Rule rule);

    // Song related public methods
    Song findSongById(Long id);

    List<Song> findAllSongs();

    Song saveSong(Song song);

    void deleteSong(Song song);

    Song getNextSong(long currentSongId);

    Song getPreviousSong(long currentSongId);

    long getMinimumSongId();

    Long getMaximumSongId();

    // Step related public methods
    Step findStepById(Long id);

    Set<Step> findStepsByPatternId(Long id);

    Step saveStep(Step step);

    void deleteStep(Step step);

    // Strike related public methods
    Strike findStrikeById(Long id);

    Set<Strike> strikesForTicker(Long tickerId);

    Player savePlayer(Player player);

    void deletePlayer(Player player);

    // Session related public methods
    Session findTickerById(Long id);

    List<Session> findAllTickers();

    Session saveTicker(Session Session);

    void deleteTicker(Long id);

    long getMinimumTickerId();

    long getMaximumTickerId();

    void deleteAllTickers();

}