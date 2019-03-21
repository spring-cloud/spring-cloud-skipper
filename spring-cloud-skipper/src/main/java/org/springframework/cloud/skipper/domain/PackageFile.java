/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Package zip file
 * @author Mark Pollack
 */
@Entity
@Table(name = "SkipperPackageFile")
public class PackageFile extends NonVersionedAbstractEntity {

	/**
	 * Package file.
	 */
	@Lob
	private byte[] packageBytes;

	public PackageFile() {
	}

	public PackageFile(byte[] packageBytes) {
		this.packageBytes = packageBytes;
	}

	public byte[] getPackageBytes() {
		return packageBytes;
	}

	public void setPackageBytes(byte[] packageBytes) {
		this.packageBytes = packageBytes;
	}
}
