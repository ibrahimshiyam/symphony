package eu.compassresearch.ide.collaboration.files;

public class FileStatus
{
	public enum FileState
	{
		ADDED, REMOVED, CHANGED, UNCHANGED, NOCONFIG, ERROR;
	}
	
	private String fileName;
	private FileState state;
	private String hash;
	
	public FileStatus(String fileName, String hash)
	{
		this(fileName, hash,  FileState.ADDED);
	}
	
	public FileStatus(String fileName, String hash, FileState state)
	{
		this.fileName = fileName;
		this.hash = hash;
		this.state = state;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getHash()
	{
		return hash;
	}

	public void setHash(String hash)
	{
		this.hash = hash;
	}

	public FileState getState()
	{
		return state;
	}

	public void setState(FileState state)
	{
		this.state = state;
	}
}
