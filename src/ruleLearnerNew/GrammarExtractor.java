/**
 * 
 */
package ruleLearnerNew;

import java.util.Set;

/**
 * @author mburroug
 * 22 Jan 2011
 * 
 * Interface which all grammar extractors must implement.
 *
 */
public interface GrammarExtractor {
	
	public Set<ExtractedRule> extract(ParseNode srcRoot,ParseNode tgtRoot);

}
