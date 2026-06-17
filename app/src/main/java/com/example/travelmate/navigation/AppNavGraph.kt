package com.example.travelmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.travelmate.ui.screens.auth.LoginScreen
import com.example.travelmate.ui.screens.auth.RegisterScreen
import com.example.travelmate.ui.screens.home.HomeScreen
import com.example.travelmate.ui.screens.places.PlaceDetailScreen
import com.example.travelmate.ui.screens.favorites.FavoritesScreen
import com.example.travelmate.ui.screens.trips.TripsScreen
import com.example.travelmate.ui.screens.trips.TripDetailScreen
import com.example.travelmate.ui.screens.trips.TripInviteScreen
import com.example.travelmate.ui.screens.trips.TripInviteInboxScreen
import com.example.travelmate.ui.screens.trips.TripChatScreen
import com.example.travelmate.ui.screens.timeline.TimelineScreen
import com.example.travelmate.ui.screens.timeline.CreatePostScreen
import com.example.travelmate.ui.screens.profile.ProfileScreen
import com.example.travelmate.ui.screens.notification.NotificationScreen
import com.example.travelmate.ui.screens.admin.AdminDashboardScreen
import com.example.travelmate.ui.screens.admin.AdminPlacesScreen
import com.example.travelmate.ui.screens.admin.AdminAddEditPlaceScreen
import com.example.travelmate.ui.screens.admin.AdminTimelineScreen
import com.example.travelmate.ui.screens.admin.AdminReviewsScreen
import com.example.travelmate.ui.screens.admin.AdminUsersScreen
import com.example.travelmate.ui.screens.admin.AdminNotificationScreen
import com.example.travelmate.ui.screens.setup.SetupScreen
import com.example.travelmate.viewmodel.AdminViewModel
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.FavoriteViewModel
import com.example.travelmate.viewmodel.NotificationViewModel
import com.example.travelmate.viewmodel.PlaceViewModel
import com.example.travelmate.viewmodel.ReviewViewModel
import com.example.travelmate.viewmodel.TravelPostViewModel
import com.example.travelmate.viewmodel.TripChatViewModel
import com.example.travelmate.viewmodel.TripInviteViewModel
import com.example.travelmate.viewmodel.TripViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val placeViewModel: PlaceViewModel = viewModel()
    val favoriteViewModel: FavoriteViewModel = viewModel()
    val tripViewModel: TripViewModel = viewModel()
    val tripInviteViewModel: TripInviteViewModel = viewModel()
    val tripChatViewModel: TripChatViewModel = viewModel()
    val notificationViewModel: NotificationViewModel = viewModel()
    val postViewModel: TravelPostViewModel = viewModel()
    val reviewViewModel: ReviewViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn = authViewModel.isLoggedIn()

    // Sau khi currentUser load xong, điều hướng đúng role
    LaunchedEffect(currentUser) {
        if (isLoggedIn && currentUser != null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == NavRoutes.HOME && currentUser?.role == "admin") {
                navController.navigate(NavRoutes.ADMIN_DASHBOARD) {
                    popUpTo(NavRoutes.HOME) { inclusive = true }
                }
            }
        }
    }

    // Start destination: luôn bắt đầu từ SETUP (splash + seed)
    val startDestination = NavRoutes.SETUP

    NavHost(navController = navController, startDestination = startDestination) {
        // Setup / Splash
        composable(NavRoutes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    val destination = when {
                        !isLoggedIn -> NavRoutes.LOGIN
                        currentUser?.role == "admin" -> NavRoutes.ADMIN_DASHBOARD
                        else -> NavRoutes.HOME
                    }
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        // Auth
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                onLoginSuccess = { user ->
                    if (user.role == "admin") {
                        navController.navigate(NavRoutes.ADMIN_DASHBOARD) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // User screens
        composable(NavRoutes.HOME) {
            HomeScreen(
                authViewModel = authViewModel,
                placeViewModel = placeViewModel,
                favoriteViewModel = favoriteViewModel,
                notificationViewModel = notificationViewModel,
                onPlaceClick = { placeId ->
                    navController.navigate(NavRoutes.placeDetail(placeId))
                },
                onNavigateToFavorites = { navController.navigate(NavRoutes.FAVORITES) },
                onNavigateToTrips = { navController.navigate(NavRoutes.TRIPS) },
                onNavigateToTimeline = { navController.navigate(NavRoutes.TIMELINE) },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                onNavigateToNotifications = { navController.navigate(NavRoutes.NOTIFICATIONS) }
            )
        }

        composable(
            route = NavRoutes.PLACE_DETAIL,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId") ?: ""
            PlaceDetailScreen(
                placeId = placeId,
                placeViewModel = placeViewModel,
                favoriteViewModel = favoriteViewModel,
                reviewViewModel = reviewViewModel,
                tripViewModel = tripViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(
                authViewModel = authViewModel,
                favoriteViewModel = favoriteViewModel,
                onPlaceClick = { placeId ->
                    navController.navigate(NavRoutes.placeDetail(placeId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.TRIPS) {
            TripsScreen(
                authViewModel = authViewModel,
                tripViewModel = tripViewModel,
                inviteViewModel = tripInviteViewModel,
                onTripClick = { tripId ->
                    navController.navigate(NavRoutes.tripDetail(tripId))
                },
                onInviteInbox = { navController.navigate(NavRoutes.TRIP_INVITE_INBOX) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripDetailScreen(
                tripId = tripId,
                tripViewModel = tripViewModel,
                placeViewModel = placeViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onPlaceClick = { placeId ->
                    navController.navigate(NavRoutes.placeDetail(placeId))
                },
                onInviteMembers = { navController.navigate(NavRoutes.tripInvite(tripId)) },
                onOpenChat = { navController.navigate(NavRoutes.tripChat(tripId)) }
            )
        }

        composable(
            route = NavRoutes.TRIP_INVITE,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripInviteScreen(
                tripId = tripId,
                authViewModel = authViewModel,
                tripViewModel = tripInviteViewModel,
                tripVm = tripViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.TRIP_INVITE_INBOX) {
            TripInviteInboxScreen(
                authViewModel = authViewModel,
                inviteViewModel = tripInviteViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.TRIP_CHAT,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripChatScreen(
                tripId = tripId,
                authViewModel = authViewModel,
                tripViewModel = tripViewModel,
                chatViewModel = tripChatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.TIMELINE) {
            TimelineScreen(
                authViewModel = authViewModel,
                postViewModel = postViewModel,
                tripViewModel = tripViewModel,
                tripInviteViewModel = tripInviteViewModel,
                onCreatePost = { navController.navigate(NavRoutes.CREATE_POST) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "create_post?tripId={tripId}&tripName={tripName}",
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                },
                navArgument("tripName") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val preselectedTripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val preselectedTripName = backStackEntry.arguments?.getString("tripName") ?: ""
            CreatePostScreen(
                authViewModel = authViewModel,
                postViewModel = postViewModel,
                tripViewModel = tripViewModel,
                preselectedTripId = preselectedTripId,
                preselectedTripName = preselectedTripName,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                favoriteViewModel = favoriteViewModel,
                tripViewModel = tripViewModel,
                postViewModel = postViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.NOTIFICATIONS) {
            NotificationScreen(
                authViewModel = authViewModel,
                notificationViewModel = notificationViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Admin screens
        composable(NavRoutes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                authViewModel = authViewModel,
                adminViewModel = adminViewModel,
                onNavigateToPlaces = { navController.navigate(NavRoutes.ADMIN_PLACES) },
                onNavigateToTimeline = { navController.navigate(NavRoutes.ADMIN_TIMELINE) },
                onNavigateToReviews = { navController.navigate(NavRoutes.ADMIN_REVIEWS) },
                onNavigateToUsers = { navController.navigate(NavRoutes.ADMIN_USERS) },
                onNavigateToNotifications = { navController.navigate(NavRoutes.ADMIN_NOTIFICATIONS) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.ADMIN_PLACES) {
            AdminPlacesScreen(
                placeViewModel = placeViewModel,
                onAddPlace = { navController.navigate(NavRoutes.ADMIN_ADD_PLACE) },
                onEditPlace = { placeId ->
                    navController.navigate(NavRoutes.adminEditPlace(placeId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADMIN_ADD_PLACE) {
            AdminAddEditPlaceScreen(
                placeId = null,
                placeViewModel = placeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ADMIN_EDIT_PLACE,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId")
            AdminAddEditPlaceScreen(
                placeId = placeId,
                placeViewModel = placeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADMIN_TIMELINE) {
            AdminTimelineScreen(
                postViewModel = postViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADMIN_REVIEWS) {
            AdminReviewsScreen(
                reviewViewModel = reviewViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADMIN_USERS) {
            AdminUsersScreen(
                adminViewModel = adminViewModel,
                tripViewModel = tripViewModel,
                postViewModel = postViewModel,
                reviewViewModel = reviewViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADMIN_NOTIFICATIONS) {
            AdminNotificationScreen(
                notificationViewModel = notificationViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
