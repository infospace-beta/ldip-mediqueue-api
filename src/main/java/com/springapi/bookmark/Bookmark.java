package com.springapi.bookmark;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.springapi.book.Book;
import com.springapi.common.entity.BaseEntity;
import com.springapi.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A user saving a book to their list. Each user can bookmark a given book only once.
 * <p>
 * {@code @OnDelete(CASCADE)} makes the database delete bookmarks automatically when their user or book is deleted.
 */
@Entity
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "book_id" }))
public class Bookmark extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Book book;

	@Column(length = 500)
	private String note;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Book getBook() {
		return book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

}
