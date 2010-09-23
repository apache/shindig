/*
 * Defines high level functionality to interact with the OpenSocial API.
 */
function SocialWrapper() {

    /*
     * Retrieves the current viewer.
     */
    this.getViewer = function(callback) {
        osapi.people.getViewer().execute(callback);
    }
    
    /*
     * Retrieves the current owner.
     */
    this.getOwner = function(callback) {
        osapi.people.getOwner().execute(callback);
    }
    
    //------------------------ ALBUMS ----------------------
    /*
     * Retrieves albums by ID(s).
     */
    this.getAlbumsById = function(userId, albumId, callback) {
        var params = {userId: userId, albumId: albumId};
        osapi.albums.get(params).execute(callback);
    }
     
    /*
     * Retrieves albums by user.
     */
    this.getAlbumsByUser = function(userId, callback) {
        osapi.albums.get({userId: userId}).execute(callback);
    }
    
    /*
     * Retrieves albums by group.
     */
    this.getAlbumsByGroup = function(userId, groupId, callback) {
        osapi.albums.get({userId: userId, groupId: groupId}).execute(callback);
    }
    
    /*
     * Creates an album for the given user.
     */
    this.createAlbum = function(userId, album, callback) {
        var params = {
            userId: userId,
            album: album
        };
        osapi.albums.create(params).execute(callback);
    }
    
    /*
     * Updates an album by ID.
     */
    this.updateAlbum = function(userId, albumId, album, callback) {
        var params = {
            userId: userId,
            albumId: albumId,
            album: album
        };
        osapi.albums.update(params).execute(callback);
    }
    
    /*
     * Deletes an album by ID.
     */
    this.deleteAlbum = function(userId, albumId, callback) {
        var params = {userId: userId, albumId: albumId};
        osapi.albums.delete(params).execute(callback);
    }
    
    //------------------------------- MEDIAITEMS ----------------------------
    /*
     * Creates a MediaItem.
     */
    this.createMediaItem = function(userId, albumId, mediaItem, callback) {
        var params = {
            userId: userId,
            albumId: albumId,
            mediaItem: mediaItem
        };
        osapi.mediaItems.create(params).execute(callback);
    }
    
    /*
     * Updates a MediaItem by ID.
     */
    this.updateMediaItem = function(userId, albumId, mediaItemId, mediaItem, callback) {
        var params = {
            userId: userId,
            albumId: albumId,
            mediaItemId: mediaItemId,
            mediaItem: mediaItem
        };
        console.log("PARAMS: " + JSON.stringify(params));
        osapi.mediaItems.update(params).execute(callback);
    }
    
    /*
     * Retrieves MediaItems by ID(s).
     */
    this.getMediaItemsById = function(userId, albumId, mediaItemId, callback) {
        var params = {
            userId: userId,
            albumId: albumId,
            mediaItemId: mediaItemId
        };
        osapi.mediaItems.get(params).execute(callback);
    }
    
    /*
     * Retrieves MediaItems by album.
     */
    this.getMediaItemsByAlbum = function(userId, albumId, callback) {
        osapi.mediaItems.get({userId: userId, albumId: albumId}).execute(callback);
    }
     
    /*
     * Retrieves MediaItems by user and group.
     */
    this.getMediaItemsByUser = function(userId, groupId, callback) {
        osapi.mediaItems.get({userId: userId, groupId: groupId}).execute(callback);
    }
     
    /*
     * Deletes a MediaItem by ID.
     */
    this.deleteMediaItem = function(userId, albumId, mediaItemId, callback) {
        var params = {
            userId: userId,
            albumId: albumId,
            mediaItemId: mediaItemId
        };
        osapi.mediaItems.delete(params).execute(callback);
    }
}