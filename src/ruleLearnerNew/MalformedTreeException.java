package ruleLearnerNew;

/**
 * @author ghannema
 * @author mburroug
 *
 */
public class MalformedTreeException extends Exception
{
	/**
	 * Serial Version UID - generated
	 */
	private static final long serialVersionUID = 4324956881688663604L;

	public MalformedTreeException()
	{
		super();
	}

	public MalformedTreeException(String message)
	{
		super(message);
	}

	public MalformedTreeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public MalformedTreeException(Throwable cause)
	{
		super(cause);
	}
}