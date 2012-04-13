package ruleLearnerNew;

import java.util.Set;

/**
 * @author ghannema
 *
 */
public interface Labeler
{
	// Returns a set of strings instead of just a plain string because of the
	// case of unary rules, where there are multiple nodes (and thus possibly
	// labels) covering the same span.
	public Set<String> getLabels(int spanStart, int spanEnd);

	// Returns the "special" string used by the labeler to denote that a span
	// has no label:
	public String noLabel();

}