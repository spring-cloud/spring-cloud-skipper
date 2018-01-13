/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Metadata for the Package.
 *
 * @author Mark Pollack
 * @author Gunnar Hillert
 */
@Entity
@Table(name = "SkipperPackageMetadata")
public class PackageMetadata extends AbstractEntity {

	/**
	 * The Package Index spec version this file is based on.
	 * we enforce apiVersion during package creation.
	 */
	@NotNull
	private String apiVersion;

	/**
	 * Indicates the origin of the repository (free form text).
	 */
	private String origin;

	/**
	 * The repository ID this Package belongs to.
	 */
	@NotNull
	private Long repositoryId;

	/**
	 * The repository name this Package belongs to.
	 */
	@NotNull
	private String repositoryName;

	/**
	 * What type of package system is being used.
	 */
	@NotNull
	private String kind;

	/**
	 * The name of the package
	 */
	@NotNull
	private String name;

	/**
	 * The display name of the package
	 */
	private String displayName;

	/**
	 * The version of the package
	 */
	@NotNull
	private String version;

	/**
	 * Location to source code for this package.
	 */
	private String packageSourceUrl;

	/**
	 * The home page of the package
	 */
	private String packageHomeUrl;

	/**
	 * Package file.
	 */
	@Lob
	@JsonIgnore
	private byte[] packageFile;

	/**
	 * A comma separated list of tags to use for searching
	 */
	private String tags;

	/**
	 * Who is maintaining this package
	 */
	private String maintainer;

	/**
	 * Brief description of the package. The packages README.md will contain more information.
	 * TODO - decide format.
	 */
	private String description;

	/**
	 * Hash of package binary that will be downloaded using SHA256 hash algorithm.
	 */
	private String sha256;

	/**
	 * Url location of a icon. TODO: size specification
	 */
	private String iconUrl;

	public PackageMetadata() {
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPackageSourceUrl() {
		return packageSourceUrl;
	}

	public void setPackageSourceUrl(String packageSourceUrl) {
		this.packageSourceUrl = packageSourceUrl;
	}

	public String getPackageHomeUrl() {
		return packageHomeUrl;
	}

	public void setPackageHomeUrl(String packageHomeUrl) {
		this.packageHomeUrl = packageHomeUrl;
	}

	@JsonIgnore
	public byte[] getPackageFileBytes() {
		return packageFile;
	}

	@JsonIgnore
	public void setPackageFileBytes(byte[] packageFileBytes) {
		this.packageFile = packageFileBytes;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getMaintainer() {
		return maintainer;
	}

	public void setMaintainer(String maintainer) {
		this.maintainer = maintainer;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public Long getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(Long repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PackageMetadata that = (PackageMetadata) o;

		if (!repositoryId.equals(that.repositoryId)) return false;
		if (!name.equals(that.name)) return false;
		return version.equals(that.version);
	}

	@Override
	public int hashCode() {
		int result = repositoryId.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + version.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "PackageMetadata{" +
				"id='" + getId() + '\'' +
				", apiVersion='" + apiVersion + '\'' +
				", origin='" + origin + '\'' +
				", repositoryName='" + repositoryName + '\'' +
				", kind='" + kind + '\'' +
				", name='" + name + '\'' +
				", version='" + version + '\'' +
				", packageSourceUrl='" + packageSourceUrl + '\'' +
				", packageHomeUrl='" + packageHomeUrl + '\'' +
				", tags='" + tags + '\'' +
				", maintainer='" + maintainer + '\'' +
				", description='" + description + '\'' +
				", sha256='" + sha256 + '\'' +
				", iconUrl='" + iconUrl + '\'' +
				'}';
	}
}
