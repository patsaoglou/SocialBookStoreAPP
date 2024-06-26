package com.SE2024.SocialBookStore.service.books;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.SE2024.SocialBookStore.dao.BookDAO;
import com.SE2024.SocialBookStore.dao.BookRequestDAO;
import com.SE2024.SocialBookStore.dao.UserProfileDAO;
import com.SE2024.SocialBookStore.dtos.BookRequestDTO;
import com.SE2024.SocialBookStore.mappers.BookRequestMapper;
import com.SE2024.SocialBookStore.model.Book;
import com.SE2024.SocialBookStore.model.BookRequest;
import com.SE2024.SocialBookStore.model.UserProfile;
import com.SE2024.SocialBookStore.service.userProfile.UserProfileServiceValidator;

@Service
public class BookRequestServiceImpl implements BookRequestService {

    @Autowired
    BookDAO bookDAO;

    @Autowired
    UserProfileDAO userProfileDAO;

    @Autowired
    BookRequestDAO bookRequestDAO;

    public void requestBook(int bookId, String username) {
        UserProfileServiceValidator userValidator = new UserProfileServiceValidator(userProfileDAO);
        BookServiceValidator bookValidator = new BookServiceValidator(bookDAO);

        UserProfile requester = userValidator.validateUsername(username);
        Book requestedBook = bookValidator.validateBookExistance(bookId);

        if (requester.isBookInUserBookOffers(requestedBook) == true) {
            throw new IllegalArgumentException("User requests from his/her book offers");
        }

        BookRequest newBookRequest = new BookRequest(requester, requestedBook);
        bookRequestDAO.save(newBookRequest);

    }

    public void deleteBookRequest(int bookId, String username) {
        UserProfileServiceValidator userValidator = new UserProfileServiceValidator(userProfileDAO);
        BookServiceValidator bookValidator = new BookServiceValidator(bookDAO);

        UserProfile requester = userValidator.validateUsername(username);
        Book requestedBook = bookValidator.validateBookExistance(bookId);

        BookRequest bookRequestToDelete = bookRequestDAO.findByRequestedBookAndRequester(requestedBook, requester);

        if (bookRequestToDelete == null) {
            throw new IllegalArgumentException("Book Request Specified does not exist");
        }

        if (bookRequestToDelete.getStatus().equals("ACCEPTED")) {
            List<BookRequest> restRequests = bookRequestDAO.findByRequestedBook(requestedBook);
            for (BookRequest request : restRequests) {
                if (!request.equals(bookRequestToDelete)) {
                    bookRequestDAO.delete(request);
                }
            }

        }

        bookRequestDAO.delete(bookRequestToDelete);
    }

    public List<BookRequestDTO> retrieveBookRequests(String username) {
        UserProfileServiceValidator userValidator = new UserProfileServiceValidator(userProfileDAO);
        UserProfile user = userValidator.validateUsername(username);

        return bookRequestDAO.findByRequester(user).stream()
                .map(BookRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<BookRequestDTO> retrieveRequestingUsers(int bookId) {
        BookServiceValidator bookValidator = new BookServiceValidator(bookDAO);
        Book requestedBook = bookValidator.validateBookExistance(bookId);

        return bookRequestDAO.findByRequestedBook(requestedBook).stream()
                .map(BookRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void selectRequester(int requestId) {
        BookRequest accepted = bookRequestDAO.findByRequestId(requestId);

        List<BookRequest> allRequests = bookRequestDAO.findByRequestedBook(accepted.getRequestedBook());

        for (BookRequest request : allRequests) {
            if (request.getRequestId() != accepted.getRequestId()) {
                request.setStatus("DECLINED");
            } else {
                request.setStatus("ACCEPTED");
            }
        }
        bookRequestDAO.saveAll(allRequests);
    }

}
