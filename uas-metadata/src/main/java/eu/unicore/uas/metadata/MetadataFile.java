package eu.unicore.uas.metadata;

import org.apache.commons.io.FilenameUtils;

public class MetadataFile {

	public static final String MD_FILE_EXTENSION = "metadata";

    public static enum MD_State {
        CONSISTENT, RESOURCE_DELETED, INCONSISTENT, CHK_CONSISTENCE, NEW
    };
   
	private static final String MD_NAME_FORMATER = ".%s."+MD_FILE_EXTENSION;

    private MD_State mdStates;

    public MD_State getMdStates() {
        return mdStates;
    }

    public void setMdStates(MD_State mdStates) {
        this.mdStates = mdStates;
    }

    /**
     * Checks if the filename is a metadata file name
     * <p>
     * Can be used to validate resource names
     * 
     * @param fileName - file name to be checked
     * @return true if the fileName is a name of a file which stores metadata
     */
    public static boolean isMetadataFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        return FilenameUtils.isExtension(fileName, MD_FILE_EXTENSION);
    }

    /**
     * Returns file name of the metadata file for a given resource
     * 
     * @param resourceName - resource name
     * @return file name of the metadata file
     */
    public static String getMetadatafileName(final String resourceName) {
        String path = FilenameUtils.getFullPath(resourceName);
        String name = FilenameUtils.getName(resourceName);
        return path + transformFileName(name);
    }

    /**
     * Returns resource name for a given metadata file name
     * <p>
     * Does the "opposite" of the @see{getMetadatafileName}
     * 
     * @param metadataFilename - metadata file name
     * @return resource for the metadata file name
     */
    public static String getResourceName(final String metadataFilename) {
        String path = FilenameUtils.getFullPath(metadataFilename);
        //from the last slash to the last dot and remove the initial dot
        String name = FilenameUtils.getBaseName(metadataFilename).substring(1);
        return path+name;
    }

    private static String transformFileName(String fileName) {
        return String.format(MD_NAME_FORMATER, fileName);
    }

}