package org.esa.cci.sst.orm;


import org.esa.cci.sst.data.Matchup;

import java.util.List;

public interface MatchupStorage {

    int getCount(MatchupQueryParameter parameter);

    List<Matchup> get(MatchupQueryParameter parameter);

    Matchup get(int matchupId);
}
