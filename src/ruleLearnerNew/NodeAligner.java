package ruleLearnerNew;

/**
 * @author mburroug
 *
 */
public interface NodeAligner {

	public NodeAlignmentList align(ParseNode srcTree,
								   ParseNode tgtTree,
								   WordAlignment wordAligns);
}
