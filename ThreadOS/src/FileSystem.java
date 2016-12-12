// Adam Croutworst and Logan McArthur
// CSS 430
// File System Project
// December 12, 2016

// source: http://courses.washington.edu/css430/prog/CSS430FinalProject.pdf
//		   Modern Operating Systems with Java Ch. 11 File System Implementation, pg 546.

public class FileSystem {
	private SuperBlock superblock;
	private Directory dir;
	private FileTable filetable;
	
	/**
	 * File System Constructor -- Instantiates the superblock, directory, file table, file size, table entry,
	 * and directory size
	 * 
	 * @param diskBlocks
	 */
	public FileSystem(int diskBlocks) {
		superblock = new SuperBlock(diskBlocks);		// Create the superblock (1st block)
		dir = new Directory(superblock.inodeBlocks);	// Create the directory
		filetable = new FileTable(dir);					// Create the file table
		
		FileTableEntry entry = 	open("/", "r");			// Start the file table with its first entry
		
		int dirSize = fsize(entry);						// Dictate the directory size
		
		if(dirSize > 0 ) {								// If valid dir size,
			byte[] dirData = new byte[dirSize];			
			read(entry, dirData);						// Read the entry 
			dir.bytes2directory(dirData);				// Convert bytes to directory
		}
		
		close(entry);									// Close the entry	
	}
	
	/**
	 * Sync the disk
	 */
	void sync() {		
		superblock.sync();					// Perform a sync call from SuperBlock
	}
	
	/**
	 * Format a disk according to the given file size
	 * 
	 * @param files
	 * @return
	 */
	boolean format(int files) {
		
		if(files >= 0) {							// If there are files,
			superblock.format(files);				// Format the disk	

			return true;							// If valid, return true
		}
		
		return false;								// Otherwise, return false
	}
	
	/**
	 * Open a file with its name and mode (permissions)
	 * 
	 * @param filename
	 * @param mode
	 * @return
	 */
	FileTableEntry open(String filename, String mode) {
		FileTableEntry fEnt = filetable.falloc(filename, mode); // Allocate file space for a new entry
		
		if(mode.equals("w")) {						
			if(deallocAllBlocks(fEnt) == false) {	
				return null;
			}
		}
		
		return fEnt;								// Return table entry
	}
	
	/**
	 * Close a file
	 * 
	 * @param fEnt
	 * @return
	 */
	public synchronized boolean close(FileTableEntry fEnt) {
		return filetable.ffree(fEnt);				// Free the entry from the file table
	}
	
	/**
	 * Return the file size
	 * 
	 * @param fEnt
	 * @return
	 */
	public synchronized int fsize(FileTableEntry fEnt) {
		return fEnt.inode.length;					// Return inode length in the entry
	}
	
	/**
	 * Read from a file given a table entry and a buffer  
	 * 
	 * @param entry
	 * @param buffer
	 * @return
	 */
	public synchronized int read(FileTableEntry entry, byte[] buffer) {
		return 0;
	}
	
	/**
	 * Write to a file given a file table and a buffer
	 * 
	 * @param fEnt
	 * @param buffer
	 * @return
	 */
	public synchronized int write(FileTableEntry entry, byte[] buffer) {
		return 0;
	}
	
	/**
	 * Deallocate all blocks within file system
	 * 
	 * @param fEnt
	 * @return
	 */
	private boolean deallocAllBlocks(FileTableEntry fEnt) {
		
		short i = 0;
		for (; i < fEnt.inode.direct.length; i++) {		// Iterate through direct blocks
			if (fEnt.inode.direct[i] != -1) {			// If not already empty,
				fEnt.inode.direct[i] = -1;				// Assign block as free	
				superblock.returnBlock(i);				// Enqueue as free block
			}
		}

		fEnt.inode.toDisk(fEnt.iNumber);				// Write blocks back to disk
						
		return true;									// If valid, return true
	}
	
	/**
	 * Delete a file
	 * 
	 * @param filename The name of the file to be deleted
	 * @return
	 */
	boolean delete(String filename) {
		
		if(filename.length() != 0) {		// If file exists,
			FileTableEntry fEnt = open(filename, "w");		// Open the file and give write permissions
			
			return dir.ifree(fEnt.iNumber) && close(fEnt);  // Perform a file deletion and close the entry
		}
		
		return false;						// File does not exist
	}
	
	private final int SEEK_SET = 0;			// Set seek pointer at beginning of file
	private final int SEEK_CUR = 1;			// Set seek pointer at current location in file
	private final int SEEK_END = 2;			// Set seek pointer at end of file
	
	/**
	 * Position seek pointer within file and return the pointer
	 * 
	 * @param fEnt 
	 * @param offset
	 * @param whence
	 * @return
	 */
	public synchronized int seek(FileTableEntry fEnt, int offset, int whence) {
		// Source: Modern Operating Systems with Java Pg. 546 Ch. 11 File System Implementation
		switch(whence) {								// Perform switch case of whence
		case SEEK_SET: fEnt.seekPtr = offset; break;	// If SET, seek pointer equals offset	
		case SEEK_CUR: fEnt.seekPtr += offset; break;	// If CUR, seek pointer increments with offset
		case SEEK_END: fEnt.seekPtr = fEnt.inode.length + offset; break; // If END, seek pointer is relative to EOF
		default: return -1; 							// Error, seek pointer stays the same
		}
		
		return fEnt.seekPtr;							// Return seek pointer
	}

	
}
