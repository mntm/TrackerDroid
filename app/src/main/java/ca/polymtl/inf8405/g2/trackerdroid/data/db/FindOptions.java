package ca.polymtl.inf8405.g2.trackerdroid.data.db;

import android.support.annotation.NonNull;

import org.bson.Document;

public class FindOptions {
    private int limit = 0;
    private Document findDocument;
    private Document projectionDocument = null;
    private Document filterDocument = null;
    private Document sortDocument = null;

    public int getLimit() {
        return limit;
    }

    private void setLimit(int limit) {
        this.limit = limit;
    }

    public Document getFindDocument() {
        return findDocument;
    }

    private void setFindDocument(Document findDocument) {
        this.findDocument = findDocument;
    }

    public Document getProjectionDocument() {
        return projectionDocument;
    }

    private void setProjectionDocument(Document projectionDocument) {
        this.projectionDocument = projectionDocument;
    }

    public Document getFilterDocument() {
        return filterDocument;
    }

    private void setFilterDocument(Document filterDocument) {
        this.filterDocument = filterDocument;
    }

    public Document getSortDocument() {
        return sortDocument;
    }

    private void setSortDocument(Document sortDocument) {
        this.sortDocument = sortDocument;
    }

    public static class Builder {
        private FindOptions findOptions;

        public Builder(@NonNull Document findDocument) {
            this.findOptions = new FindOptions();
            this.findOptions.setFindDocument(findDocument);
        }

        public Builder setLimit(int limit) {
            this.findOptions.setLimit(limit);
            return this;
        }

        public Builder setFindDocument(@NonNull Document findDocument) {
            this.findOptions.setFindDocument(findDocument);
            return this;
        }

        public Builder setProjectionDocument(@NonNull Document projectionDocument) {
            this.findOptions.setProjectionDocument(projectionDocument);
            return this;
        }

        public Builder setFilterDocument(@NonNull Document filterDocument) {
            this.findOptions.setFilterDocument(filterDocument);
            return this;
        }

        public Builder setSortDocument(@NonNull Document sortDocument) {
            this.findOptions.setSortDocument(sortDocument);
            return this;
        }

        public FindOptions build() {
            FindOptions ret = new FindOptions();
            ret.setSortDocument(this.findOptions.getSortDocument());
            ret.setFilterDocument(this.findOptions.getFilterDocument());
            ret.setFindDocument(this.findOptions.getFindDocument());
            ret.setProjectionDocument(this.findOptions.getProjectionDocument());
            ret.setLimit(this.findOptions.getLimit());
            return ret;
        }
    }
}