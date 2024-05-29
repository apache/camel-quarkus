/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.graphql.it;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.graphql.it.model.Book;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@ApplicationScoped
public class BooksGraphQLResource {
    private static final List<Book> BOOKS = new ArrayList<>(List.of(
            new Book(1, "Harry Potter and the Philosophers Stone", "J.K Rowling"),
            new Book(2, "Moby Dick", "Herman Melville"),
            new Book(3, "Interview with the vampire", "Anne Rice")));

    @Query
    public List<Book> getBooks() {
        return BOOKS;
    }

    @Query
    public Book getBookById(int id) {
        return BOOKS.stream().filter(book -> book.getId() == id).findFirst().orElse(null);
    }

    @Mutation
    public Book addBook(Book bookInput) {
        Book book = new Book(bookInput.getId(), bookInput.getName(), bookInput.getAuthor());
        BOOKS.add(book);
        return book;
    }
}
