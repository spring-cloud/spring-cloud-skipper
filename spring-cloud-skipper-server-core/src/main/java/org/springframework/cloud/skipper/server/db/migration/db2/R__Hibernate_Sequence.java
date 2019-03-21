/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.db.migration.db2;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.skipper.server.db.migration.AbstractMigration;
import org.springframework.cloud.skipper.server.db.migration.SqlCommand;

/**
 * Repeatable migration ensuring that {@code hibernate_sequence} table exists.
 * Done for {@code db2} via java and suppressing error as it doesn't support
 * "create sequence if not exists".
 *
 * @author Janne Valkealahti
 *
 */
public class R__Hibernate_Sequence extends AbstractMigration {

	// Caused by: org.springframework.jdbc.BadSqlGrammarException:
	// StatementCallback; bad SQL grammar [create sequence hibernate_sequence start with 1 increment by 1];
	// nested exception is com.ibm.db2.jcc.am.SqlSyntaxErrorException:
	// DB2 SQL Error: SQLCODE=-601, SQLSTATE=42710, SQLERRMC=HIBERNATE_SEQUENCE;SEQUENCE, DRIVER=4.19.26
	private final static List<SqlCommand> commands = Arrays.asList(
			SqlCommand.from("create sequence hibernate_sequence start with 1 increment by 1", -601));

	public R__Hibernate_Sequence() {
		super(commands);
	}
}
