package id.diskola.app.utils.session

import id.diskola.app.dataclass.ResponData.LoginSchoolData
import id.diskola.app.dataclass.ResponData.LoginSsoSchoolData
import id.diskola.app.dataclass.ResponData.SchoolDetail
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.dataclass.ResponData.SekolahItem

/** Every endpoint that returns "a school" uses its own response shape — these map each one to the
 * single [SessionSchool] shape the app stores/reads from prefs. */

fun SchoolItem.toSession() = SessionSchool(
    id = id,
    uuid = uuid,
    name = name,
    image = image,
    address = address.orEmpty(),
    cityName = city_name.orEmpty(),
    coordinateRadius = coordinate_radius ?: "50",
    coordinateLatitude = coordinate_latitude ?: 0.0,
    coordinateLongitude = coordinate_longitude ?: 0.0,
)

fun SchoolDetail.toSession() = SessionSchool(
    id = id,
    uuid = uuid,
    name = name.orEmpty(),
    image = image.orEmpty(),
    address = address.orEmpty(),
    coordinateRadius = coordinate_radius ?: "50",
    coordinateLatitude = coordinate_latitude ?: 0.0,
    coordinateLongitude = coordinate_longitude ?: 0.0,
)

fun LoginSchoolData.toSession(fallback: SessionSchool) = SessionSchool(
    id = id ?: fallback.id,
    uuid = uuid ?: fallback.uuid,
    name = name ?: fallback.name,
    image = image ?: fallback.image,
    address = fallback.address,
    cityName = fallback.cityName,
    coordinateRadius = fallback.coordinateRadius,
    coordinateLatitude = fallback.coordinateLatitude,
    coordinateLongitude = fallback.coordinateLongitude,
)

fun SekolahItem.toSession() = SessionSchool(
    id = id,
    uuid = uuid,
    name = name,
    image = image,
    address = address,
    coordinateRadius = coordinate_radius ?: "50",
    coordinateLatitude = coordinate_latitude ?: 0.0,
    coordinateLongitude = coordinate_longitude ?: 0.0,
)

fun LoginSsoSchoolData.toSession(fallback: SessionSchool) = SessionSchool(
    id = school_id.takeIf { it > 0 } ?: fallback.id,
    uuid = fallback.uuid,
    name = name.ifBlank { fallback.name },
    image = fallback.image,
    address = fallback.address,
    cityName = fallback.cityName,
    coordinateRadius = fallback.coordinateRadius,
    coordinateLatitude = fallback.coordinateLatitude,
    coordinateLongitude = fallback.coordinateLongitude,
)
